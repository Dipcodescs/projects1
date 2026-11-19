package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.DeliveryStatus
import com.example.data.model.MarketplaceOrder
import com.example.data.model.PayoutEntryType
import com.example.data.model.PayoutLedgerEntry
import com.example.data.model.UserPrivateProfile
import com.example.data.model.UserRole
import com.example.data.model.formatInr
import com.example.data.model.formatKg
import com.example.data.util.openGoogleMapsDirections
import com.example.ui.components.DeliveryProgressStepper
import com.example.ui.components.DeliveryRouteMapPreview
import com.example.ui.components.EmptyStateCard
import com.example.ui.i18n.LocalizedCopy
import com.example.ui.theme.HarvestForest
import com.example.ui.theme.TerracottaClay
import com.example.ui.theme.TransitBlue
import com.example.ui.theme.VerifiedEmerald
import com.example.ui.viewmodel.DriverEarningsSummary

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DriverHubScreen(
    copy: LocalizedCopy,
    userProfile: UserPrivateProfile,
    availableTasks: List<MarketplaceOrder>,
    assignedOrders: List<MarketplaceOrder>,
    earnings: DriverEarningsSummary,
    payoutLedger: List<PayoutLedgerEntry>,
    isBusy: Boolean,
    onRegisterAsDriver: (vehicleType: String, vehiclePlate: String) -> Unit,
    onToggleOnline: (Boolean) -> Unit,
    onAcceptTask: (String) -> Unit,
    onAdvanceStatus: (MarketplaceOrder, DeliveryStatus) -> Unit,
    onWithdraw: (amount: Double, note: String) -> Unit
) {
    var selectedSubTab by remember { mutableIntStateOf(0) } // 0=Available, 1=Active Runs, 2=Earnings
    var showWithdrawDialog by remember { mutableStateOf(false) }

    val activeRuns = assignedOrders.filter { it.deliveryStatusEnum() != DeliveryStatus.DELIVERED }
    val completedRuns = assignedOrders.filter { it.deliveryStatusEnum() == DeliveryStatus.DELIVERED }

    if (userProfile.roleEnum() != UserRole.DRIVER) {
        DriverRegistrationView(
            userProfile = userProfile,
            isBusy = isBusy,
            onRegister = onRegisterAsDriver
        )
        return
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("driver_hub_list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Driver Status & Vehicle Control Card
        item {
            Card(
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.LocalShipping,
                                contentDescription = null,
                                tint = TransitBlue,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = copy.navDriverHub,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = "${userProfile.vehicleType.ifBlank { "Cargo Vehicle" }} · ${userProfile.vehiclePlate.ifBlank { "Registered" }} · ${userProfile.city}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = if (userProfile.driverActive) "Online & receiving farm dispatch tasks"
                            else "Offline — toggle online to accept new routes",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (userProfile.driverActive) VerifiedEmerald else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Switch(
                        checked = userProfile.driverActive,
                        onCheckedChange = onToggleOnline,
                        modifier = Modifier.testTag("driver_online_switch")
                    )
                }
            }
        }

        // Sub-navigation pills: Available Tasks | Active Runs | Earnings
        item {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = selectedSubTab == 0,
                    onClick = { selectedSubTab = 0 },
                    label = { Text("Available (${availableTasks.size})") },
                    modifier = Modifier.testTag("driver_tab_available")
                )
                FilterChip(
                    selected = selectedSubTab == 1,
                    onClick = { selectedSubTab = 1 },
                    label = { Text("Active Runs (${activeRuns.size})") },
                    modifier = Modifier.testTag("driver_tab_active")
                )
                FilterChip(
                    selected = selectedSubTab == 2,
                    onClick = { selectedSubTab = 2 },
                    label = { Text("Earnings · ${formatInr(earnings.availableBalance)}") },
                    modifier = Modifier.testTag("driver_tab_earnings")
                )
            }
        }

        when (selectedSubTab) {
            0 -> {
                if (availableTasks.isEmpty()) {
                    item {
                        EmptyStateCard(
                            icon = Icons.Filled.LocalShipping,
                            title = "No open pickup tasks right now",
                            subtitle = "As soon as a consumer checks out fresh farm produce, the pickup & drop-off task with guaranteed payout will appear here live."
                        )
                    }
                } else {
                    items(availableTasks, key = { "avail_${it.id}" }) { task ->
                        AvailableDeliveryTaskCard(
                            copy = copy,
                            order = task,
                            canAccept = !isBusy && userProfile.driverActive,
                            onAccept = {
                                onAcceptTask(task.id)
                                selectedSubTab = 1
                            }
                        )
                    }
                }
            }

            1 -> {
                if (activeRuns.isEmpty() && completedRuns.isEmpty()) {
                    item {
                        EmptyStateCard(
                            icon = Icons.Filled.Navigation,
                            title = "No assigned deliveries yet",
                            subtitle = "Accept an available farm pickup task to start navigating routes and earning 100% of delivery payouts."
                        )
                    }
                } else {
                    if (activeRuns.isNotEmpty()) {
                        item {
                            Text(
                                text = "In-Progress Deliveries (${activeRuns.size})",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        items(activeRuns, key = { "run_${it.id}" }) { order ->
                            ActiveDriverDeliveryCard(
                                copy = copy,
                                order = order,
                                isBusy = isBusy,
                                onAdvance = { next -> onAdvanceStatus(order, next) }
                            )
                        }
                    }

                    if (completedRuns.isNotEmpty()) {
                        item {
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "Recently Completed (${completedRuns.size})",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        items(completedRuns, key = { "done_${it.id}" }) { order ->
                            CompletedDriverRunCard(order = order)
                        }
                    }
                }
            }

            else -> {
                item {
                    DriverEarningsHeroCard(
                        copy = copy,
                        earnings = earnings,
                        onOpenWithdraw = { showWithdrawDialog = true }
                    )
                }
                item {
                    Text(
                        text = "Payout & Withdrawal Ledger (${payoutLedger.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                if (payoutLedger.isEmpty()) {
                    item {
                        EmptyStateCard(
                            icon = Icons.Filled.AccountBalanceWallet,
                            title = "No payout transactions yet",
                            subtitle = "Completing a delivery automatically credits 100% of the delivery fee to your earnings ledger."
                        )
                    }
                } else {
                    items(payoutLedger, key = { it.id }) { entry ->
                        PayoutLedgerRowCard(entry = entry)
                    }
                }
            }
        }
    }

    if (showWithdrawDialog) {
        WithdrawEarningsDialog(
            availableBalance = earnings.availableBalance,
            isBusy = isBusy,
            onDismiss = { showWithdrawDialog = false },
            onConfirm = { amount, note ->
                onWithdraw(amount, note)
                showWithdrawDialog = false
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DriverRegistrationView(
    userProfile: UserPrivateProfile,
    isBusy: Boolean,
    onRegister: (vehicleType: String, vehiclePlate: String) -> Unit
) {
    var vehicleType by remember {
        mutableStateOf(userProfile.vehicleType.ifBlank { "Mini Truck" })
    }
    var vehiclePlate by remember {
        mutableStateOf(userProfile.vehiclePlate.ifBlank { "MH15AB1234" })
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Card(
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.LocalShipping,
                            contentDescription = null,
                            tint = TransitBlue,
                            modifier = Modifier.size(28.dp)
                        )
                        Column {
                            Text(
                                text = "Delivery Driver Registration",
                                style = MaterialTheme.typography.headlineMedium
                            )
                            Text(
                                text = "Earn 100% of delivery payouts transporting farm harvests directly to local households.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Text("Select Vehicle Type", style = MaterialTheme.typography.titleSmall)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("Mini Truck", "Cargo Van", "E-Rickshaw", "Two-Wheeler").forEach { type ->
                            FilterChip(
                                selected = vehicleType == type,
                                onClick = { vehicleType = type },
                                label = { Text(type) }
                            )
                        }
                    }

                    OutlinedTextField(
                        value = vehiclePlate,
                        onValueChange = { if (it.length <= 20) vehiclePlate = it.uppercase() },
                        label = { Text("Vehicle License Plate") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("driver_reg_plate_input")
                    )

                    Button(
                        onClick = { onRegister(vehicleType, vehiclePlate) },
                        enabled = !isBusy && vehiclePlate.trim().length >= 4,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .testTag("driver_register_submit_button"),
                        shape = MaterialTheme.shapes.large
                    ) {
                        Text("Register & Go Online as Driver", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun AvailableDeliveryTaskCard(
    copy: LocalizedCopy,
    order: MarketplaceOrder,
    canAccept: Boolean,
    onAccept: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("available_task_card_${order.id}"),
        shape = MaterialTheme.shapes.large,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = order.cropSummary(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Load: ${formatKg(order.totalWeightKg)} · Route: ${order.distanceKm} km",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = "Earn ${formatInr(order.driverPayoutAmount)}",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
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

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Filled.LocationOn, contentDescription = null, tint = HarvestForest, modifier = Modifier.size(16.dp))
                    Text("Pickup: ${order.pickupAddress}", style = MaterialTheme.typography.bodySmall)
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Filled.LocationOn, contentDescription = null, tint = TerracottaClay, modifier = Modifier.size(16.dp))
                    Text("Drop-off: ${order.deliveryAddress}", style = MaterialTheme.typography.bodySmall)
                }
            }

            Button(
                onClick = onAccept,
                enabled = canAccept,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("accept_task_button_${order.id}"),
                shape = MaterialTheme.shapes.medium
            ) {
                Text(
                    text = if (canAccept) "${copy.acceptDelivery} · ${formatInr(order.driverPayoutAmount)}"
                    else "Go Online Above to Accept Task",
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun ActiveDriverDeliveryCard(
    copy: LocalizedCopy,
    order: MarketplaceOrder,
    isBusy: Boolean,
    onAdvance: (DeliveryStatus) -> Unit
) {
    val context = LocalContext.current
    val status = order.deliveryStatusEnum()
    val (nextStatus, nextLabel) = when (status) {
        DeliveryStatus.ASSIGNED -> DeliveryStatus.PICKED_UP to copy.markPickedUp
        DeliveryStatus.PICKED_UP -> DeliveryStatus.EN_ROUTE to copy.markEnRoute
        DeliveryStatus.EN_ROUTE -> DeliveryStatus.DELIVERED to "${copy.markDelivered} (+${formatInr(order.driverPayoutAmount)})"
        else -> null to ""
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("active_delivery_card_${order.id}"),
        shape = MaterialTheme.shapes.large,
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${order.farmName} → ${order.consumerName}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${order.cropSummary()} · ${order.distanceKm} km",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = formatInr(order.driverPayoutAmount),
                    style = MaterialTheme.typography.titleMedium,
                    color = VerifiedEmerald,
                    fontWeight = FontWeight.Bold
                )
            }

            DeliveryProgressStepper(currentStatus = status)

            DeliveryRouteMapPreview(
                pickupLat = order.pickupLat,
                pickupLng = order.pickupLng,
                pickupLabel = order.farmName,
                dropLat = order.deliveryLat,
                dropLng = order.deliveryLng,
                dropLabel = order.deliveryAddress,
                distanceKm = order.distanceKm
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        val isHeadingToPickup = status == DeliveryStatus.ASSIGNED
                        openGoogleMapsDirections(
                            context = context,
                            destLat = if (isHeadingToPickup) order.pickupLat else order.deliveryLat,
                            destLng = if (isHeadingToPickup) order.pickupLng else order.deliveryLng,
                            label = if (isHeadingToPickup) order.farmName else order.deliveryAddress
                        )
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("driver_navigate_button_${order.id}")
                ) {
                    Icon(Icons.Filled.Directions, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(if (status == DeliveryStatus.ASSIGNED) "Nav Pickup" else "Nav Drop-off")
                }

                if (nextStatus != null) {
                    Button(
                        onClick = { onAdvance(nextStatus) },
                        enabled = !isBusy,
                        modifier = Modifier
                            .weight(1.4f)
                            .height(48.dp)
                            .testTag("advance_status_button_${order.id}"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (nextStatus == DeliveryStatus.DELIVERED) VerifiedEmerald
                            else MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text(nextLabel, fontWeight = FontWeight.Bold, maxLines = 1)
                    }
                }
            }
        }
    }
}

@Composable
private fun CompletedDriverRunCard(order: MarketplaceOrder) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = VerifiedEmerald)
                Column {
                    Text(
                        text = "${order.farmName} → ${order.consumerName}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${order.cropSummary()} · ${order.distanceKm} km",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Text(
                text = "+${formatInr(order.driverPayoutAmount)}",
                style = MaterialTheme.typography.titleSmall,
                color = VerifiedEmerald,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun DriverEarningsHeroCard(
    copy: LocalizedCopy,
    earnings: DriverEarningsSummary,
    onOpenWithdraw: () -> Unit
) {
    Card(
        shape = MaterialTheme.shapes.large,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        listOf(HarvestForest, Color(0xFF2D6A4F))
                    )
                )
                .padding(20.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column {
                        Text(
                            text = "Available to Withdraw",
                            style = MaterialTheme.typography.labelLarge,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                        Text(
                            text = formatInr(earnings.availableBalance),
                            style = MaterialTheme.typography.displayLarge,
                            color = Color.White
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = Color.White.copy(alpha = 0.16f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Filled.Verified, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                            Text(
                                text = "${earnings.completedCount} Trips",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                HorizontalDivider(color = Color.White.copy(alpha = 0.2f))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Lifetime Earned", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.8f))
                        Text(formatInr(earnings.totalEarned), style = MaterialTheme.typography.titleMedium, color = Color.White, fontWeight = FontWeight.Bold)
                    }
                    Column {
                        Text("In-Transit Pending", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.8f))
                        Text(formatInr(earnings.pendingInTransit), style = MaterialTheme.typography.titleMedium, color = Color.White, fontWeight = FontWeight.Bold)
                    }
                    Column {
                        Text("Withdrawn", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.8f))
                        Text(formatInr(earnings.totalWithdrawn), style = MaterialTheme.typography.titleMedium, color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }

                Button(
                    onClick = onOpenWithdraw,
                    enabled = earnings.availableBalance > 0.0,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("driver_withdraw_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                ) {
                    Text(copy.withdrawEarnings, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun PayoutLedgerRowCard(entry: PayoutLedgerEntry) {
    val isWithdrawal = entry.typeEnum() == PayoutEntryType.WITHDRAWAL
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = if (isWithdrawal) Icons.Filled.ArrowUpward else Icons.Filled.ArrowDownward,
                    contentDescription = entry.typeEnum().label,
                    tint = if (isWithdrawal) TerracottaClay else VerifiedEmerald
                )
                Column {
                    Text(
                        text = entry.typeEnum().label,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = entry.note,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Text(
                text = (if (isWithdrawal) "-" else "+") + formatInr(entry.amount),
                style = MaterialTheme.typography.titleSmall,
                color = if (isWithdrawal) TerracottaClay else VerifiedEmerald,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun WithdrawEarningsDialog(
    availableBalance: Double,
    isBusy: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (amount: Double, note: String) -> Unit
) {
    var upiId by remember { mutableStateOf("driver@upi") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Withdraw Driver Earnings") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Record payout of ${formatInr(availableBalance)} to your linked UPI or bank account.",
                    style = MaterialTheme.typography.bodyMedium
                )
                OutlinedTextField(
                    value = upiId,
                    onValueChange = { if (it.length <= 80) upiId = it },
                    label = { Text("UPI ID / Bank Account Reference") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("withdraw_upi_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(availableBalance, "Bank/UPI Payout to $upiId") },
                enabled = !isBusy && availableBalance > 0.0 && upiId.trim().isNotEmpty(),
                modifier = Modifier.testTag("confirm_withdraw_button")
            ) {
                Text("Confirm ${formatInr(availableBalance)}")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

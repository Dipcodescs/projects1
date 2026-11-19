package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.DeliveryStatus
import com.example.data.model.ProduceCategory
import com.example.data.model.SplitSettlementSummary
import com.example.data.model.UserRole
import com.example.data.model.formatInr
import com.example.data.util.decodeBase64Bitmap
import com.example.ui.theme.StarAmber
import com.example.ui.theme.TransitBlue
import com.example.ui.theme.VerifiedEmerald

@Composable
fun CropImageDisplay(
    photoBase64: String?,
    category: ProduceCategory,
    contentDescription: String,
    modifier: Modifier = Modifier
) {
    val decodedBitmap = remember(photoBase64) { decodeBase64Bitmap(photoBase64) }
    val fallbackRes = when (category) {
        ProduceCategory.VEGETABLES -> R.drawable.img_crop_vegetables
        ProduceCategory.FRUITS -> R.drawable.img_crop_fruits
        ProduceCategory.GRAINS -> R.drawable.img_crop_grains
        ProduceCategory.DAIRY_HERBS -> R.drawable.img_crop_vegetables
    }

    if (decodedBitmap != null) {
        Image(
            bitmap = decodedBitmap.asImageBitmap(),
            contentDescription = contentDescription,
            modifier = modifier,
            contentScale = ContentScale.Crop
        )
    } else {
        Image(
            painter = painterResource(id = fallbackRes),
            contentDescription = contentDescription,
            modifier = modifier,
            contentScale = ContentScale.Crop
        )
    }
}

@Composable
fun StarRatingBadge(
    averageRating: Double,
    reviewCount: Long,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(999.dp),
        color = MaterialTheme.colorScheme.tertiaryContainer
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Star,
                contentDescription = "Farmer rating",
                tint = StarAmber,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = if (reviewCount > 0L) {
                    String.format("%.1f (%d)", averageRating, reviewCount)
                } else {
                    "New Farm"
                },
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onTertiaryContainer,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
fun InteractiveStarRatingBar(
    rating: Int,
    onRatingSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    starSize: Dp = 36.dp
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (star in 1..5) {
            Box(
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .clip(CircleShape)
                    .clickable { onRatingSelected(star) }
                    .testTag("rating_star_$star"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (star <= rating) Icons.Filled.Star else Icons.Outlined.StarOutline,
                    contentDescription = "Rate $star stars",
                    tint = if (star <= rating) StarAmber else MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(starSize)
                )
            }
        }
    }
}

@Composable
fun RoleBadgeChip(
    role: UserRole,
    modifier: Modifier = Modifier
) {
    val (bg, fg) = when (role) {
        UserRole.FARMER -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
        UserRole.CONSUMER -> MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer
        UserRole.DRIVER -> TransitBlue.copy(alpha = 0.14f) to TransitBlue
    }
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(999.dp),
        color = bg
    ) {
        Text(
            text = role.label,
            style = MaterialTheme.typography.labelMedium,
            color = fg,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun DeliveryProgressStepper(
    currentStatus: DeliveryStatus,
    modifier: Modifier = Modifier
) {
    val steps = listOf(
        DeliveryStatus.READY_FOR_PICKUP to "Ready",
        DeliveryStatus.ASSIGNED to "Assigned",
        DeliveryStatus.PICKED_UP to "Picked Up",
        DeliveryStatus.EN_ROUTE to "En Route",
        DeliveryStatus.DELIVERED to "Delivered"
    )
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            steps.forEachIndexed { idx, (step, _) ->
                val done = currentStatus.stepIndex >= step.stepIndex
                val active = currentStatus == step
                Box(
                    modifier = Modifier
                        .size(if (active) 24.dp else 18.dp)
                        .clip(CircleShape)
                        .background(
                            when {
                                done -> MaterialTheme.colorScheme.primary
                                else -> MaterialTheme.colorScheme.surfaceVariant
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (done) {
                        Icon(
                            imageVector = if (step == DeliveryStatus.DELIVERED) Icons.Filled.CheckCircle else Icons.Filled.LocalShipping,
                            contentDescription = step.label,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }
                if (idx < steps.lastIndex) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(3.dp)
                            .background(
                                if (currentStatus.stepIndex > step.stepIndex) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant
                                }
                            )
                    )
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            steps.forEach { (step, shortLabel) ->
                val active = currentStatus.stepIndex >= step.stepIndex
                Text(
                    text = shortLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = if (currentStatus == step) FontWeight.Bold else FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun SplitPaymentBreakdownCard(
    summary: SplitSettlementSummary,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Verified,
                    contentDescription = "Direct split settlement",
                    tint = VerifiedEmerald,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "Transparent Split Settlement",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }
            BreakdownRow("Direct Farmer Payout (96%)", formatInr(summary.farmerDirectPayout), highlight = true)
            BreakdownRow("Delivery Driver Payout (100% to driver)", formatInr(summary.driverDeliveryPayout))
            BreakdownRow("Platform Route Operations (4%)", formatInr(summary.platformFee))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            BreakdownRow("Total Payable", formatInr(summary.grandTotal), bold = true)
            if (summary.estimatedMiddlemanSavings > 0.0) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = "You save ${formatInr(summary.estimatedMiddlemanSavings)} vs typical middleman retail prices",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun BreakdownRow(
    label: String,
    value: String,
    highlight: Boolean = false,
    bold: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = if (bold) MaterialTheme.typography.titleSmall else MaterialTheme.typography.bodyMedium,
            color = if (highlight) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = if (bold || highlight) FontWeight.SemiBold else FontWeight.Normal
        )
        Text(
            text = value,
            style = if (bold) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
            color = if (highlight) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            fontWeight = if (bold || highlight) FontWeight.Bold else FontWeight.Medium
        )
    }
}

@Composable
fun EmptyStateCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (action != null) {
                Spacer(Modifier.height(4.dp))
                action()
            }
        }
    }
}

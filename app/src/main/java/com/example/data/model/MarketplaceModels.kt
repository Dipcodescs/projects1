package com.example.data.model

import org.json.JSONArray
import org.json.JSONObject

enum class UserRole(val label: String) {
    CONSUMER("Consumer"),
    FARMER("Farmer"),
    DRIVER("Delivery Driver");

    companion object {
        fun safeValueOf(raw: String?): UserRole =
            entries.firstOrNull { it.name == raw } ?: CONSUMER
    }
}

enum class KycStatus(val label: String) {
    UNVERIFIED("Unverified"),
    PENDING("Under Review"),
    VERIFIED("KYC Verified");

    companion object {
        fun safeValueOf(raw: String?): KycStatus =
            entries.firstOrNull { it.name == raw } ?: UNVERIFIED
    }
}

enum class ProduceCategory(val label: String) {
    VEGETABLES("Vegetables"),
    FRUITS("Fruits"),
    GRAINS("Grains & Pulses"),
    DAIRY_HERBS("Dairy & Herbs");

    companion object {
        fun safeValueOf(raw: String?): ProduceCategory =
            entries.firstOrNull { it.name == raw } ?: VEGETABLES
    }
}

enum class ListingStatus {
    ACTIVE,
    SOLD_OUT,
    ARCHIVED;

    companion object {
        fun safeValueOf(raw: String?): ListingStatus =
            entries.firstOrNull { it.name == raw } ?: ACTIVE
    }
}

enum class DeliveryStatus(val label: String, val stepIndex: Int) {
    READY_FOR_PICKUP("Ready for Pickup", 0),
    ASSIGNED("Driver Assigned", 1),
    PICKED_UP("Picked Up from Farm", 2),
    EN_ROUTE("En Route to Buyer", 3),
    DELIVERED("Delivered", 4);

    companion object {
        fun safeValueOf(raw: String?): DeliveryStatus =
            entries.firstOrNull { it.name == raw } ?: READY_FOR_PICKUP
    }
}

enum class PayoutEntryType(val label: String) {
    DELIVERY_EARNING("Delivery Earning"),
    FARMER_SETTLEMENT("Farm Settlement"),
    WITHDRAWAL("Bank Withdrawal");

    companion object {
        fun safeValueOf(raw: String?): PayoutEntryType =
            entries.firstOrNull { it.name == raw } ?: DELIVERY_EARNING
    }
}

data class UserPrivateProfile(
    val id: String = "",
    val userId: String = "",
    val displayName: String = "",
    val role: String = UserRole.CONSUMER.name,
    val phone: String = "",
    val kycStatus: String = KycStatus.VERIFIED.name,
    val farmName: String = "",
    val city: String = "Nashik",
    val defaultAddress: String = "",
    val latitude: Double = 19.9975,
    val longitude: Double = 73.7898,
    val languageCode: String = "en",
    val vehicleType: String = "",
    val vehiclePlate: String = "",
    val driverActive: Boolean = false,
    val createdAtMillis: Long = 0L,
    val updatedAtMillis: Long = 0L
) {
    fun roleEnum(): UserRole = UserRole.safeValueOf(role)
    fun kycEnum(): KycStatus = KycStatus.safeValueOf(kycStatus)
}

data class PublicProfile(
    val id: String = "",
    val userId: String = "",
    val displayName: String = "",
    val role: String = UserRole.CONSUMER.name,
    val farmName: String = "",
    val city: String = "Nashik",
    val latitude: Double = 19.9975,
    val longitude: Double = 73.7898,
    val kycVerified: Boolean = true,
    val averageRating: Double = 0.0,
    val ratingSum: Long = 0L,
    val reviewCount: Long = 0L,
    val completedDeliveries: Long = 0L,
    val vehicleType: String = "",
    val visibility: String = "PUBLIC",
    val lastReviewedOrderId: String? = null,
    val createdAtMillis: Long = 0L,
    val updatedAtMillis: Long = 0L
) {
    fun roleEnum(): UserRole = UserRole.safeValueOf(role)
}

data class ProduceListing(
    val id: String = "",
    val farmerId: String = "",
    val farmerName: String = "",
    val farmName: String = "",
    val cropName: String = "",
    val category: String = ProduceCategory.VEGETABLES.name,
    val pricePerKg: Double = 0.0,
    val marketComparePricePerKg: Double = 0.0,
    val availableQtyKg: Double = 0.0,
    val harvestDate: String = "",
    val description: String = "",
    val city: String = "",
    val latitude: Double = 19.9975,
    val longitude: Double = 73.7898,
    val photoBase64: String? = null,
    val status: String = ListingStatus.ACTIVE.name,
    val createdAtMillis: Long = 0L,
    val updatedAtMillis: Long = 0L
) {
    fun categoryEnum(): ProduceCategory = ProduceCategory.safeValueOf(category)
    fun statusEnum(): ListingStatus = ListingStatus.safeValueOf(status)
    fun middlemanSavingsPercent(): Int {
        if (marketComparePricePerKg <= pricePerKg || marketComparePricePerKg <= 0.0) return 0
        return (((marketComparePricePerKg - pricePerKg) / marketComparePricePerKg) * 100.0).toInt().coerceIn(0, 90)
    }
}

data class OrderItem(
    val listingId: String = "",
    val cropName: String = "",
    val quantityKg: Double = 0.0,
    val pricePerKg: Double = 0.0,
    val lineTotal: Double = 0.0,
    val farmerPayoutAmount: Double = 0.0
) {
    fun toJsonObject(): JSONObject = JSONObject().apply {
        put("listingId", listingId)
        put("cropName", cropName)
        put("quantityKg", quantityKg)
        put("pricePerKg", pricePerKg)
        put("lineTotal", lineTotal)
        put("farmerPayoutAmount", farmerPayoutAmount)
    }

    companion object {
        fun toJsonString(items: List<OrderItem>): String {
            val array = JSONArray()
            items.forEach { array.put(it.toJsonObject()) }
            return array.toString()
        }

        fun listFromJson(json: String): List<OrderItem> {
            if (json.isBlank()) return emptyList()
            return try {
                val array = JSONArray(json)
                buildList(array.length()) {
                    for (i in 0 until array.length()) {
                        val obj = array.optJSONObject(i) ?: continue
                        add(
                            OrderItem(
                                listingId = obj.optString("listingId", ""),
                                cropName = obj.optString("cropName", ""),
                                quantityKg = obj.optDouble("quantityKg", 0.0),
                                pricePerKg = obj.optDouble("pricePerKg", 0.0),
                                lineTotal = obj.optDouble("lineTotal", 0.0),
                                farmerPayoutAmount = obj.optDouble("farmerPayoutAmount", 0.0)
                            )
                        )
                    }
                }
            } catch (_: Exception) {
                emptyList()
            }
        }
    }
}

data class MarketplaceOrder(
    val id: String = "",
    val consumerId: String = "",
    val consumerName: String = "",
    val farmerId: String = "",
    val farmerName: String = "",
    val farmName: String = "",
    val driverId: String = "",
    val driverName: String = "",
    val itemsList: List<OrderItem> = emptyList(),
    val totalWeightKg: Double = 0.0,
    val subtotalAmount: Double = 0.0,
    val farmerPayoutAmount: Double = 0.0,
    val driverPayoutAmount: Double = 0.0,
    val platformFeeAmount: Double = 0.0,
    val totalAmount: Double = 0.0,
    val paymentStatus: String = "PAID_SPLIT_SETTLED",
    val paymentRef: String = "",
    val deliveryStatus: String = DeliveryStatus.READY_FOR_PICKUP.name,
    val pickupAddress: String = "",
    val pickupLat: Double = 19.9975,
    val pickupLng: Double = 73.7898,
    val deliveryAddress: String = "",
    val deliveryLat: Double = 20.0059,
    val deliveryLng: Double = 73.7609,
    val distanceKm: Double = 0.0,
    val reviewed: Boolean = false,
    val createdAtMillis: Long = 0L,
    val updatedAtMillis: Long = 0L,
    val deliveredAtMillis: Long? = null
) {
    fun deliveryStatusEnum(): DeliveryStatus = DeliveryStatus.safeValueOf(deliveryStatus)
    fun parsedItems(): List<OrderItem> = itemsList
    fun cropSummary(): String = itemsList.joinToString(", ") {
        "${it.cropName} (${formatKg(it.quantityKg)})"
    }.take(120).ifBlank { "Fresh Farm Produce" }
}

data class FarmerReview(
    val id: String = "",
    val orderId: String = "",
    val farmerId: String = "",
    val farmerName: String = "",
    val consumerId: String = "",
    val consumerName: String = "",
    val rating: Long = 5L,
    val comment: String = "",
    val cropSummary: String = "",
    val verifiedPurchase: Boolean = true,
    val createdAtMillis: Long = 0L
)

data class PayoutLedgerEntry(
    val id: String = "",
    val orderId: String = "",
    val entryType: String = PayoutEntryType.DELIVERY_EARNING.name,
    val amount: Double = 0.0,
    val status: String = "CREDITED",
    val note: String = "",
    val createdAtMillis: Long = 0L
) {
    fun typeEnum(): PayoutEntryType = PayoutEntryType.safeValueOf(entryType)
}

data class CartEntry(
    val listing: ProduceListing,
    val quantityKg: Double
) {
    val lineTotal: Double get() = listing.pricePerKg * quantityKg
}

data class ListingWithMeta(
    val listing: ProduceListing,
    val distanceKm: Double,
    val farmerProfile: PublicProfile?
)

data class SplitSettlementSummary(
    val subtotal: Double,
    val farmerDirectPayout: Double,
    val driverDeliveryPayout: Double,
    val platformFee: Double,
    val grandTotal: Double,
    val estimatedMiddlemanSavings: Double
)

fun formatKg(kg: Double): String =
    if (kg % 1.0 == 0.0) "${kg.toInt()} kg" else String.format("%.1f kg", kg)

fun formatInr(amount: Double): String =
    if (amount % 1.0 == 0.0) "₹${amount.toInt()}" else String.format("₹%.2f", amount)

package com.example.data.repository

import android.content.Context
import androidx.room.withTransaction
import com.example.data.local.FarmerReviewEntity
import com.example.data.local.KisanDirectDatabase
import com.example.data.local.OrderEntity
import com.example.data.local.PayoutLedgerEntity
import com.example.data.local.ProduceListingEntity
import com.example.data.local.UserProfileEntity
import com.example.data.model.CartEntry
import com.example.data.model.DeliveryStatus
import com.example.data.model.FarmerReview
import com.example.data.model.KycStatus
import com.example.data.model.ListingStatus
import com.example.data.model.MarketplaceOrder
import com.example.data.model.OrderItem
import com.example.data.model.PayoutEntryType
import com.example.data.model.PayoutLedgerEntry
import com.example.data.model.ProduceCategory
import com.example.data.model.ProduceListing
import com.example.data.model.PublicProfile
import com.example.data.model.UserPrivateProfile
import com.example.data.model.UserRole
import com.example.data.util.computeDriverDeliveryFee
import com.example.data.util.haversineDistanceKm
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import kotlin.math.roundToInt

class MarketplaceRepository(
    private val database: KisanDirectDatabase
) {
    constructor(context: Context) : this(KisanDirectDatabase.getInstance(context))

    private val dao = database.dao()

    companion object {
        const val LOCAL_USER_ID = "local_kisan_user"
    }

    // --- Reactive Local Room Observation Streams ---

    fun observeUserProfile(userId: String = LOCAL_USER_ID): Flow<UserPrivateProfile?> =
        dao.observeUserProfile(userId).map { it?.toPrivateProfile() }

    fun observePublicProfiles(): Flow<List<PublicProfile>> =
        dao.observeAllProfiles().map { list -> list.map { it.toPublicProfile() } }

    fun observeActiveListings(): Flow<List<ProduceListing>> =
        dao.observeActiveListings().map { list -> list.map { it.toDomain() } }

    fun observeFarmerListings(farmerId: String = LOCAL_USER_ID): Flow<List<ProduceListing>> =
        dao.observeFarmerListings(farmerId).map { list -> list.map { it.toDomain() } }

    fun observeConsumerOrders(consumerId: String = LOCAL_USER_ID): Flow<List<MarketplaceOrder>> =
        dao.observeConsumerOrders(consumerId).map { list -> list.map { it.toDomain() } }

    fun observeFarmerOrders(farmerId: String = LOCAL_USER_ID): Flow<List<MarketplaceOrder>> =
        dao.observeFarmerOrders(farmerId).map { list -> list.map { it.toDomain() } }

    fun observeAvailableDeliveryTasks(): Flow<List<MarketplaceOrder>> =
        dao.observeAvailableDeliveryTasks().map { list -> list.map { it.toDomain() } }

    fun observeDriverAssignedOrders(driverId: String = LOCAL_USER_ID): Flow<List<MarketplaceOrder>> =
        dao.observeDriverAssignedOrders(driverId).map { list -> list.map { it.toDomain() } }

    fun observeVerifiedReviews(): Flow<List<FarmerReview>> =
        dao.observeVerifiedReviews().map { list -> list.map { it.toDomain() } }

    fun observePayoutLedger(userId: String = LOCAL_USER_ID): Flow<List<PayoutLedgerEntry>> =
        dao.observePayoutLedger(userId).map { list -> list.map { it.toDomain() } }

    // --- Profile, Role Onboarding & Preferences ---

    suspend fun saveUserProfile(
        userId: String = LOCAL_USER_ID,
        displayName: String,
        role: UserRole,
        phone: String,
        farmName: String,
        city: String,
        defaultAddress: String,
        latitude: Double,
        longitude: Double,
        languageCode: String,
        vehicleType: String,
        vehiclePlate: String,
        driverActive: Boolean,
        kycStatus: KycStatus = KycStatus.VERIFIED
    ): Result<Unit> {
        return try {
            val cleanName = displayName.trim().take(60)
            require(cleanName.isNotEmpty()) { "Display name is required." }
            val cleanPhone = phone.trim().take(20)
            val cleanFarm = farmName.trim().take(80)
            val cleanCity = city.trim().take(60).ifEmpty { "Nashik" }
            val cleanAddress = defaultAddress.trim().take(200)
            val cleanVehicle = vehicleType.trim().take(40)
            val cleanPlate = vehiclePlate.trim().take(20)
            val cleanLang = if (languageCode in listOf("en", "hi", "ta", "mr")) languageCode else "en"
            val clampedLat = latitude.coerceIn(-90.0, 90.0)
            val clampedLng = longitude.coerceIn(-180.0, 180.0)
            val now = System.currentTimeMillis()

            database.withTransaction {
                val existing = dao.getUserProfile(userId)
                val entity = UserProfileEntity(
                    userId = userId,
                    displayName = cleanName,
                    role = role.name,
                    phone = cleanPhone,
                    kycStatus = kycStatus.name,
                    farmName = cleanFarm,
                    city = cleanCity,
                    defaultAddress = cleanAddress,
                    latitude = clampedLat,
                    longitude = clampedLng,
                    languageCode = cleanLang,
                    vehicleType = cleanVehicle,
                    vehiclePlate = cleanPlate,
                    driverActive = driverActive,
                    averageRating = existing?.averageRating ?: 0.0,
                    ratingSum = existing?.ratingSum ?: 0L,
                    reviewCount = existing?.reviewCount ?: 0L,
                    completedDeliveries = existing?.completedDeliveries ?: 0L,
                    lastReviewedOrderId = existing?.lastReviewedOrderId,
                    createdAtMillis = existing?.createdAtMillis ?: now,
                    updatedAtMillis = now
                )
                dao.upsertUserProfile(entity)
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateDriverAvailability(
        active: Boolean,
        userId: String = LOCAL_USER_ID
    ): Result<Unit> {
        return try {
            dao.updateDriverAvailability(userId, active, System.currentTimeMillis())
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateLanguage(
        languageCode: String,
        userId: String = LOCAL_USER_ID
    ): Result<Unit> {
        return try {
            val cleanLang = if (languageCode in listOf("en", "hi", "ta", "mr")) languageCode else "en"
            dao.updateLanguage(userId, cleanLang, System.currentTimeMillis())
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // --- Farmer Produce Listings (Role-Protected) ---

    suspend fun publishListing(
        profile: UserPrivateProfile,
        cropName: String,
        category: ProduceCategory,
        pricePerKg: Double,
        marketComparePricePerKg: Double,
        availableQtyKg: Double,
        harvestDate: String,
        description: String,
        city: String,
        latitude: Double,
        longitude: Double,
        photoBase64: String?
    ): Result<String> {
        return try {
            require(profile.roleEnum() == UserRole.FARMER) {
                "Switch to Farmer role in Account to publish produce listings."
            }
            val cleanCrop = cropName.trim().take(60)
            require(cleanCrop.length >= 2) { "Crop name must be at least 2 characters." }
            require(pricePerKg > 0.0 && pricePerKg <= 100000.0) { "Enter a valid price per kg." }
            require(availableQtyKg > 0.0 && availableQtyKg <= 50000.0) { "Enter a valid quantity in kg." }

            val cleanCompare = marketComparePricePerKg.coerceAtLeast(pricePerKg).coerceAtMost(150000.0)
            val now = System.currentTimeMillis()
            val id = "crop_${UUID.randomUUID().toString().replace("-", "").take(12)}"
            val farmerId = profile.userId.ifBlank { LOCAL_USER_ID }

            dao.upsertListing(
                ProduceListingEntity(
                    id = id,
                    farmerId = farmerId,
                    farmerName = profile.displayName.trim().take(60).ifEmpty { "Farmer" },
                    farmName = profile.farmName.trim().take(80).ifEmpty { "${profile.displayName}'s Farm" },
                    cropName = cleanCrop,
                    category = category.name,
                    pricePerKg = round2(pricePerKg),
                    marketComparePricePerKg = round2(cleanCompare),
                    availableQtyKg = round2(availableQtyKg),
                    harvestDate = harvestDate.trim().take(32).let { if (it.length >= 4) it else "Today" },
                    description = description.trim().take(400),
                    city = city.trim().take(60).ifEmpty { profile.city.ifEmpty { "Nashik" } },
                    latitude = latitude.coerceIn(-90.0, 90.0),
                    longitude = longitude.coerceIn(-180.0, 180.0),
                    photoBase64 = photoBase64?.takeIf { it.isNotBlank() && it.length <= 75000 },
                    status = ListingStatus.ACTIVE.name,
                    createdAtMillis = now,
                    updatedAtMillis = now
                )
            )
            Result.success(id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateListingInventory(
        listingId: String,
        pricePerKg: Double,
        availableQtyKg: Double
    ): Result<Unit> {
        return try {
            require(listingId.isNotBlank()) { "Listing ID is required." }
            require(pricePerKg > 0.0 && pricePerKg <= 100000.0) { "Invalid price per kg." }
            require(availableQtyKg >= 0.0 && availableQtyKg <= 50000.0) { "Invalid quantity." }

            val newStatus = if (availableQtyKg <= 0.0) ListingStatus.SOLD_OUT.name else ListingStatus.ACTIVE.name
            dao.updateListingInventory(
                listingId = listingId,
                pricePerKg = round2(pricePerKg),
                availableQtyKg = round2(availableQtyKg),
                status = newStatus,
                updatedAt = System.currentTimeMillis()
            )
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteListing(listingId: String): Result<Unit> {
        return try {
            require(listingId.isNotBlank()) { "Listing ID is required." }
            dao.deleteListing(listingId)
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // --- Atomic Race-Condition-Free Checkout with Split Settlement ---

    suspend fun placeOrdersAtomically(
        consumerProfile: UserPrivateProfile,
        cartEntries: List<CartEntry>,
        deliveryAddress: String,
        deliveryLat: Double,
        deliveryLng: Double
    ): Result<List<String>> {
        return try {
            require(cartEntries.isNotEmpty()) { "Cart is empty." }
            val cleanAddress = deliveryAddress.trim().take(200)
            require(cleanAddress.length >= 3) { "Please enter a delivery address." }
            require(cartEntries.all { it.quantityKg > 0.0 }) { "Each item quantity must be greater than zero." }

            val groupedByFarmer = cartEntries.groupBy { it.listing.farmerId }
            require(groupedByFarmer.values.all { it.size <= 20 }) { "Maximum 20 items per farm order." }

            val consumerId = consumerProfile.userId.ifBlank { LOCAL_USER_ID }
            val createdOrderIds = mutableListOf<String>()

            database.withTransaction {
                val now = System.currentTimeMillis()
                val lockedListings = mutableMapOf<String, ProduceListingEntity>()

                // 1. Validate latest stock atomically inside SQLite transaction
                for (entry in cartEntries) {
                    val current = dao.getListingById(entry.listing.id)
                        ?: throw IllegalStateException("${entry.listing.cropName} is no longer available.")

                    if (ListingStatus.safeValueOf(current.status) != ListingStatus.ACTIVE) {
                        throw IllegalStateException("${current.cropName} is currently sold out.")
                    }
                    if (current.availableQtyKg < entry.quantityKg) {
                        throw IllegalStateException(
                            "Insufficient stock for ${current.cropName}: only ${current.availableQtyKg} kg left."
                        )
                    }
                    lockedListings[entry.listing.id] = current
                }

                // 2. Atomically decrement inventory
                for (entry in cartEntries) {
                    val current = lockedListings.getValue(entry.listing.id)
                    val remaining = round2((current.availableQtyKg - entry.quantityKg).coerceAtLeast(0.0))
                    val nextStatus = if (remaining <= 0.0) ListingStatus.SOLD_OUT.name else ListingStatus.ACTIVE.name
                    dao.updateListingStockAfterCheckout(
                        listingId = entry.listing.id,
                        remainingQtyKg = remaining,
                        status = nextStatus,
                        updatedAt = now
                    )
                }

                // 3. Create farmer-scoped orders with transparent 96% farmer / 100% driver / 4% platform split
                val timestampSuffix = now.toString().takeLast(8)
                groupedByFarmer.entries.forEachIndexed { idx, (farmerId, entries) ->
                    val first = lockedListings.getValue(entries.first().listing.id)
                    val routeKm = haversineDistanceKm(
                        first.latitude, first.longitude,
                        deliveryLat, deliveryLng
                    )
                    val orderItems = entries.map { entry ->
                        val latest = lockedListings.getValue(entry.listing.id)
                        val lineTotal = round2(latest.pricePerKg * entry.quantityKg)
                        val itemFarmerPayout = round2(lineTotal * 0.96)
                        OrderItem(
                            listingId = latest.id,
                            cropName = latest.cropName,
                            quantityKg = round2(entry.quantityKg),
                            pricePerKg = latest.pricePerKg,
                            lineTotal = lineTotal,
                            farmerPayoutAmount = itemFarmerPayout
                        )
                    }
                    val totalWeight = round2(entries.sumOf { it.quantityKg })
                    val subtotal = round2(entries.sumOf {
                        lockedListings.getValue(it.listing.id).pricePerKg * it.quantityKg
                    })
                    val farmerPayout = round2(subtotal * 0.96)
                    val platformFee = round2((subtotal - farmerPayout).coerceAtLeast(0.0))
                    val driverFee = computeDriverDeliveryFee(routeKm, totalWeight)
                    val grandTotal = round2(subtotal + driverFee)

                    val orderId = "ord_${UUID.randomUUID().toString().replace("-", "").take(12)}"
                    createdOrderIds.add(orderId)

                    dao.insertOrder(
                        OrderEntity(
                            id = orderId,
                            consumerId = consumerId,
                            consumerName = consumerProfile.displayName.trim().take(60).ifEmpty { "Buyer" },
                            farmerId = farmerId,
                            farmerName = first.farmerName.take(60).ifEmpty { "Farmer" },
                            farmName = first.farmName.take(80).ifEmpty { "Local Farm" },
                            driverId = "",
                            driverName = "",
                            itemsJson = OrderItem.toJsonString(orderItems),
                            totalWeightKg = totalWeight,
                            subtotalAmount = subtotal,
                            farmerPayoutAmount = farmerPayout,
                            driverPayoutAmount = driverFee,
                            platformFeeAmount = platformFee,
                            totalAmount = grandTotal,
                            paymentStatus = "PAID_SPLIT_SETTLED",
                            paymentRef = "KD_SPLIT_${timestampSuffix}_$idx",
                            deliveryStatus = DeliveryStatus.READY_FOR_PICKUP.name,
                            pickupAddress = "${first.farmName}, ${first.city}".take(160),
                            pickupLat = first.latitude.coerceIn(-90.0, 90.0),
                            pickupLng = first.longitude.coerceIn(-180.0, 180.0),
                            deliveryAddress = cleanAddress,
                            deliveryLat = deliveryLat.coerceIn(-90.0, 90.0),
                            deliveryLng = deliveryLng.coerceIn(-180.0, 180.0),
                            distanceKm = routeKm.coerceIn(0.0, 5000.0),
                            reviewed = false,
                            createdAtMillis = now,
                            updatedAtMillis = now,
                            deliveredAtMillis = null
                        )
                    )
                }
            }
            Result.success(createdOrderIds)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // --- Delivery Driver Task Dispatch & Earnings Ledger ---

    suspend fun acceptDeliveryTask(orderId: String, driverProfile: UserPrivateProfile): Result<Unit> {
        return try {
            require(driverProfile.roleEnum() == UserRole.DRIVER) {
                "Switch to Delivery Driver role to accept delivery tasks."
            }
            val driverId = driverProfile.userId.ifBlank { LOCAL_USER_ID }
            val updatedRows = dao.claimDeliveryTask(
                orderId = orderId,
                driverId = driverId,
                driverName = driverProfile.displayName.trim().take(60).ifEmpty { "Driver" },
                updatedAt = System.currentTimeMillis()
            )
            if (updatedRows == 0) {
                throw IllegalStateException("This delivery task has already been claimed.")
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun advanceDeliveryStatus(
        order: MarketplaceOrder,
        nextStatus: DeliveryStatus,
        driverId: String = LOCAL_USER_ID
    ): Result<Unit> {
        return try {
            val now = System.currentTimeMillis()
            when (nextStatus) {
                DeliveryStatus.PICKED_UP -> {
                    val updated = dao.advanceDeliveryStatus(
                        orderId = order.id,
                        driverId = driverId,
                        expectedCurrentStatus = DeliveryStatus.ASSIGNED.name,
                        nextStatus = DeliveryStatus.PICKED_UP.name,
                        updatedAt = now
                    )
                    if (updated == 0) throw IllegalStateException("Order must be Assigned before Pickup.")
                }

                DeliveryStatus.EN_ROUTE -> {
                    val updated = dao.advanceDeliveryStatus(
                        orderId = order.id,
                        driverId = driverId,
                        expectedCurrentStatus = DeliveryStatus.PICKED_UP.name,
                        nextStatus = DeliveryStatus.EN_ROUTE.name,
                        updatedAt = now
                    )
                    if (updated == 0) throw IllegalStateException("Order must be Picked Up before En Route.")
                }

                DeliveryStatus.DELIVERED -> {
                    database.withTransaction {
                        val updated = dao.markOrderDelivered(
                            orderId = order.id,
                            driverId = driverId,
                            deliveredAt = now
                        )
                        if (updated == 0) {
                            throw IllegalStateException("Order must be En Route before marking Delivered.")
                        }
                        dao.incrementCompletedDeliveries(driverId, now)
                        dao.insertPayout(
                            PayoutLedgerEntity(
                                id = "earn_${order.id}",
                                userId = driverId,
                                orderId = order.id,
                                entryType = PayoutEntryType.DELIVERY_EARNING.name,
                                amount = order.driverPayoutAmount.coerceAtLeast(1.0),
                                status = "CREDITED",
                                note = "Delivered: ${order.farmName} -> ${order.consumerName}".take(120),
                                createdAtMillis = now
                            )
                        )
                    }
                }

                else -> throw IllegalStateException("Invalid delivery state transition.")
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun recordWithdrawal(
        amount: Double,
        destinationNote: String,
        userId: String = LOCAL_USER_ID
    ): Result<Unit> {
        return try {
            require(amount > 0.0) { "No withdrawable balance available." }
            val id = "wd_${UUID.randomUUID().toString().replace("-", "").take(12)}"
            dao.insertPayout(
                PayoutLedgerEntity(
                    id = id,
                    userId = userId,
                    orderId = "",
                    entryType = PayoutEntryType.WITHDRAWAL.name,
                    amount = round2(amount),
                    status = "WITHDRAWN",
                    note = destinationNote.trim().take(120).ifEmpty { "Instant UPI / Bank Payout" },
                    createdAtMillis = System.currentTimeMillis()
                )
            )
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // --- Verified Purchase Farmer Ratings & Reviews (Atomic Transaction) ---

    suspend fun submitVerifiedFarmerReview(
        order: MarketplaceOrder,
        rating: Int,
        comment: String,
        consumerId: String = LOCAL_USER_ID
    ): Result<Unit> {
        return try {
            require(rating in 1..5) { "Rating must be between 1 and 5 stars." }
            val cleanComment = comment.trim().take(500)
            require(cleanComment.length >= 3) { "Please write at least 3 characters in your review." }
            require(order.deliveryStatusEnum() == DeliveryStatus.DELIVERED) {
                "You can only review a farmer after your order is delivered."
            }
            require(!order.reviewed) { "You have already reviewed this verified order." }

            val now = System.currentTimeMillis()
            database.withTransaction {
                val latestOrder = dao.getOrderById(order.id)
                    ?: throw IllegalStateException("Order not found.")

                if (latestOrder.consumerId != consumerId) {
                    throw IllegalStateException("Only the verified buyer can review this order.")
                }
                if (latestOrder.deliveryStatus != DeliveryStatus.DELIVERED.name || latestOrder.reviewed) {
                    throw IllegalStateException("This order cannot be reviewed again.")
                }

                dao.insertReview(
                    FarmerReviewEntity(
                        orderId = order.id,
                        farmerId = order.farmerId,
                        farmerName = order.farmerName.take(60).ifEmpty { "Farmer" },
                        consumerId = consumerId,
                        consumerName = order.consumerName.take(60).ifEmpty { "Verified Buyer" },
                        rating = rating.toLong(),
                        comment = cleanComment,
                        cropSummary = order.cropSummary(),
                        verifiedPurchase = true,
                        createdAtMillis = now
                    )
                )

                val marked = dao.markOrderReviewed(
                    orderId = order.id,
                    consumerId = consumerId,
                    updatedAt = now
                )
                if (marked == 0) {
                    throw IllegalStateException("This order has already been reviewed.")
                }

                val farmerProfile = dao.getUserProfile(order.farmerId)
                if (farmerProfile != null) {
                    val nextSum = farmerProfile.ratingSum + rating.toLong()
                    val nextCount = farmerProfile.reviewCount + 1L
                    val nextAverage = ((nextSum.toDouble() / nextCount.toDouble()) * 10.0).roundToInt() / 10.0
                    dao.updateFarmerRatingStats(
                        farmerId = order.farmerId,
                        ratingSum = nextSum,
                        reviewCount = nextCount,
                        averageRating = nextAverage,
                        orderId = order.id,
                        updatedAt = now
                    )
                }
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun round2(value: Double): Double = (value * 100.0).roundToInt() / 100.0
}

package com.example.data.local

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.FarmerReview
import com.example.data.model.MarketplaceOrder
import com.example.data.model.OrderItem
import com.example.data.model.PayoutLedgerEntry
import com.example.data.model.ProduceListing
import com.example.data.model.PublicProfile
import com.example.data.model.UserPrivateProfile
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "user_profiles")
data class UserProfileEntity(
    @PrimaryKey val userId: String,
    val displayName: String,
    val role: String,
    val phone: String,
    val kycStatus: String,
    val farmName: String,
    val city: String,
    val defaultAddress: String,
    val latitude: Double,
    val longitude: Double,
    val languageCode: String,
    val vehicleType: String,
    val vehiclePlate: String,
    val driverActive: Boolean,
    val averageRating: Double = 0.0,
    val ratingSum: Long = 0L,
    val reviewCount: Long = 0L,
    val completedDeliveries: Long = 0L,
    val lastReviewedOrderId: String? = null,
    val createdAtMillis: Long,
    val updatedAtMillis: Long
) {
    fun toPrivateProfile(): UserPrivateProfile = UserPrivateProfile(
        id = userId,
        userId = userId,
        displayName = displayName,
        role = role,
        phone = phone,
        kycStatus = kycStatus,
        farmName = farmName,
        city = city,
        defaultAddress = defaultAddress,
        latitude = latitude,
        longitude = longitude,
        languageCode = languageCode,
        vehicleType = vehicleType,
        vehiclePlate = vehiclePlate,
        driverActive = driverActive,
        createdAtMillis = createdAtMillis,
        updatedAtMillis = updatedAtMillis
    )

    fun toPublicProfile(): PublicProfile = PublicProfile(
        id = userId,
        userId = userId,
        displayName = displayName,
        role = role,
        farmName = farmName,
        city = city,
        latitude = latitude,
        longitude = longitude,
        kycVerified = kycStatus == "VERIFIED",
        averageRating = averageRating,
        ratingSum = ratingSum,
        reviewCount = reviewCount,
        completedDeliveries = completedDeliveries,
        vehicleType = vehicleType,
        visibility = "PUBLIC",
        lastReviewedOrderId = lastReviewedOrderId,
        createdAtMillis = createdAtMillis,
        updatedAtMillis = updatedAtMillis
    )
}

@Entity(tableName = "produce_listings")
data class ProduceListingEntity(
    @PrimaryKey val id: String,
    val farmerId: String,
    val farmerName: String,
    val farmName: String,
    val cropName: String,
    val category: String,
    val pricePerKg: Double,
    val marketComparePricePerKg: Double,
    val availableQtyKg: Double,
    val harvestDate: String,
    val description: String,
    val city: String,
    val latitude: Double,
    val longitude: Double,
    val photoBase64: String?,
    val status: String,
    val createdAtMillis: Long,
    val updatedAtMillis: Long
) {
    fun toDomain(): ProduceListing = ProduceListing(
        id = id,
        farmerId = farmerId,
        farmerName = farmerName,
        farmName = farmName,
        cropName = cropName,
        category = category,
        pricePerKg = pricePerKg,
        marketComparePricePerKg = marketComparePricePerKg,
        availableQtyKg = availableQtyKg,
        harvestDate = harvestDate,
        description = description,
        city = city,
        latitude = latitude,
        longitude = longitude,
        photoBase64 = photoBase64,
        status = status,
        createdAtMillis = createdAtMillis,
        updatedAtMillis = updatedAtMillis
    )
}

@Entity(tableName = "orders")
data class OrderEntity(
    @PrimaryKey val id: String,
    val consumerId: String,
    val consumerName: String,
    val farmerId: String,
    val farmerName: String,
    val farmName: String,
    val driverId: String,
    val driverName: String,
    val itemsJson: String,
    val totalWeightKg: Double,
    val subtotalAmount: Double,
    val farmerPayoutAmount: Double,
    val driverPayoutAmount: Double,
    val platformFeeAmount: Double,
    val totalAmount: Double,
    val paymentStatus: String,
    val paymentRef: String,
    val deliveryStatus: String,
    val pickupAddress: String,
    val pickupLat: Double,
    val pickupLng: Double,
    val deliveryAddress: String,
    val deliveryLat: Double,
    val deliveryLng: Double,
    val distanceKm: Double,
    val reviewed: Boolean,
    val createdAtMillis: Long,
    val updatedAtMillis: Long,
    val deliveredAtMillis: Long?
) {
    fun toDomain(): MarketplaceOrder = MarketplaceOrder(
        id = id,
        consumerId = consumerId,
        consumerName = consumerName,
        farmerId = farmerId,
        farmerName = farmerName,
        farmName = farmName,
        driverId = driverId,
        driverName = driverName,
        itemsList = OrderItem.listFromJson(itemsJson),
        totalWeightKg = totalWeightKg,
        subtotalAmount = subtotalAmount,
        farmerPayoutAmount = farmerPayoutAmount,
        driverPayoutAmount = driverPayoutAmount,
        platformFeeAmount = platformFeeAmount,
        totalAmount = totalAmount,
        paymentStatus = paymentStatus,
        paymentRef = paymentRef,
        deliveryStatus = deliveryStatus,
        pickupAddress = pickupAddress,
        pickupLat = pickupLat,
        pickupLng = pickupLng,
        deliveryAddress = deliveryAddress,
        deliveryLat = deliveryLat,
        deliveryLng = deliveryLng,
        distanceKm = distanceKm,
        reviewed = reviewed,
        createdAtMillis = createdAtMillis,
        updatedAtMillis = updatedAtMillis,
        deliveredAtMillis = deliveredAtMillis
    )
}

@Entity(tableName = "farmer_reviews")
data class FarmerReviewEntity(
    @PrimaryKey val orderId: String,
    val farmerId: String,
    val farmerName: String,
    val consumerId: String,
    val consumerName: String,
    val rating: Long,
    val comment: String,
    val cropSummary: String,
    val verifiedPurchase: Boolean,
    val createdAtMillis: Long
) {
    fun toDomain(): FarmerReview = FarmerReview(
        id = orderId,
        orderId = orderId,
        farmerId = farmerId,
        farmerName = farmerName,
        consumerId = consumerId,
        consumerName = consumerName,
        rating = rating,
        comment = comment,
        cropSummary = cropSummary,
        verifiedPurchase = verifiedPurchase,
        createdAtMillis = createdAtMillis
    )
}

@Entity(tableName = "payout_ledger")
data class PayoutLedgerEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val orderId: String,
    val entryType: String,
    val amount: Double,
    val status: String,
    val note: String,
    val createdAtMillis: Long
) {
    fun toDomain(): PayoutLedgerEntry = PayoutLedgerEntry(
        id = id,
        orderId = orderId,
        entryType = entryType,
        amount = amount,
        status = status,
        note = note,
        createdAtMillis = createdAtMillis
    )
}

@Dao
interface KisanDirectDao {

    // --- User Profiles ---

    @Query("SELECT * FROM user_profiles WHERE userId = :userId LIMIT 1")
    fun observeUserProfile(userId: String): Flow<UserProfileEntity?>

    @Query("SELECT * FROM user_profiles WHERE userId = :userId LIMIT 1")
    suspend fun getUserProfile(userId: String): UserProfileEntity?

    @Query("SELECT * FROM user_profiles")
    fun observeAllProfiles(): Flow<List<UserProfileEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertUserProfile(entity: UserProfileEntity)

    @Query("UPDATE user_profiles SET driverActive = :active, updatedAtMillis = :updatedAt WHERE userId = :userId")
    suspend fun updateDriverAvailability(userId: String, active: Boolean, updatedAt: Long)

    @Query("UPDATE user_profiles SET languageCode = :languageCode, updatedAtMillis = :updatedAt WHERE userId = :userId")
    suspend fun updateLanguage(userId: String, languageCode: String, updatedAt: Long)

    @Query(
        """
        UPDATE user_profiles
        SET completedDeliveries = completedDeliveries + 1, updatedAtMillis = :updatedAt
        WHERE userId = :userId
        """
    )
    suspend fun incrementCompletedDeliveries(userId: String, updatedAt: Long)

    @Query(
        """
        UPDATE user_profiles
        SET ratingSum = :ratingSum,
            reviewCount = :reviewCount,
            averageRating = :averageRating,
            lastReviewedOrderId = :orderId,
            updatedAtMillis = :updatedAt
        WHERE userId = :farmerId
        """
    )
    suspend fun updateFarmerRatingStats(
        farmerId: String,
        ratingSum: Long,
        reviewCount: Long,
        averageRating: Double,
        orderId: String,
        updatedAt: Long
    )

    // --- Produce Listings ---

    @Query("SELECT * FROM produce_listings WHERE status = 'ACTIVE' ORDER BY createdAtMillis DESC")
    fun observeActiveListings(): Flow<List<ProduceListingEntity>>

    @Query("SELECT * FROM produce_listings WHERE farmerId = :farmerId ORDER BY createdAtMillis DESC")
    fun observeFarmerListings(farmerId: String): Flow<List<ProduceListingEntity>>

    @Query("SELECT * FROM produce_listings WHERE id = :listingId LIMIT 1")
    suspend fun getListingById(listingId: String): ProduceListingEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertListing(entity: ProduceListingEntity)

    @Query(
        """
        UPDATE produce_listings
        SET pricePerKg = :pricePerKg,
            availableQtyKg = :availableQtyKg,
            status = :status,
            updatedAtMillis = :updatedAt
        WHERE id = :listingId
        """
    )
    suspend fun updateListingInventory(
        listingId: String,
        pricePerKg: Double,
        availableQtyKg: Double,
        status: String,
        updatedAt: Long
    )

    @Query(
        """
        UPDATE produce_listings
        SET availableQtyKg = :remainingQtyKg,
            status = :status,
            updatedAtMillis = :updatedAt
        WHERE id = :listingId
        """
    )
    suspend fun updateListingStockAfterCheckout(
        listingId: String,
        remainingQtyKg: Double,
        status: String,
        updatedAt: Long
    )

    @Query("DELETE FROM produce_listings WHERE id = :listingId")
    suspend fun deleteListing(listingId: String)

    // --- Orders & Dispatch ---

    @Query("SELECT * FROM orders WHERE consumerId = :consumerId ORDER BY createdAtMillis DESC")
    fun observeConsumerOrders(consumerId: String): Flow<List<OrderEntity>>

    @Query("SELECT * FROM orders WHERE farmerId = :farmerId ORDER BY createdAtMillis DESC")
    fun observeFarmerOrders(farmerId: String): Flow<List<OrderEntity>>

    @Query(
        """
        SELECT * FROM orders
        WHERE deliveryStatus = 'READY_FOR_PICKUP' AND driverId = ''
        ORDER BY createdAtMillis DESC
        """
    )
    fun observeAvailableDeliveryTasks(): Flow<List<OrderEntity>>

    @Query("SELECT * FROM orders WHERE driverId = :driverId ORDER BY createdAtMillis DESC")
    fun observeDriverAssignedOrders(driverId: String): Flow<List<OrderEntity>>

    @Query("SELECT * FROM orders WHERE id = :orderId LIMIT 1")
    suspend fun getOrderById(orderId: String): OrderEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertOrder(entity: OrderEntity)

    @Query(
        """
        UPDATE orders
        SET driverId = :driverId,
            driverName = :driverName,
            deliveryStatus = 'ASSIGNED',
            updatedAtMillis = :updatedAt
        WHERE id = :orderId AND deliveryStatus = 'READY_FOR_PICKUP' AND driverId = ''
        """
    )
    suspend fun claimDeliveryTask(
        orderId: String,
        driverId: String,
        driverName: String,
        updatedAt: Long
    ): Int

    @Query(
        """
        UPDATE orders
        SET deliveryStatus = :nextStatus,
            updatedAtMillis = :updatedAt
        WHERE id = :orderId AND driverId = :driverId AND deliveryStatus = :expectedCurrentStatus
        """
    )
    suspend fun advanceDeliveryStatus(
        orderId: String,
        driverId: String,
        expectedCurrentStatus: String,
        nextStatus: String,
        updatedAt: Long
    ): Int

    @Query(
        """
        UPDATE orders
        SET deliveryStatus = 'DELIVERED',
            deliveredAtMillis = :deliveredAt,
            updatedAtMillis = :deliveredAt
        WHERE id = :orderId AND driverId = :driverId AND deliveryStatus = 'EN_ROUTE'
        """
    )
    suspend fun markOrderDelivered(
        orderId: String,
        driverId: String,
        deliveredAt: Long
    ): Int

    @Query(
        """
        UPDATE orders
        SET reviewed = 1, updatedAtMillis = :updatedAt
        WHERE id = :orderId AND consumerId = :consumerId AND deliveryStatus = 'DELIVERED' AND reviewed = 0
        """
    )
    suspend fun markOrderReviewed(
        orderId: String,
        consumerId: String,
        updatedAt: Long
    ): Int

    // --- Verified Farmer Reviews ---

    @Query("SELECT * FROM farmer_reviews WHERE verifiedPurchase = 1 ORDER BY createdAtMillis DESC")
    fun observeVerifiedReviews(): Flow<List<FarmerReviewEntity>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertReview(entity: FarmerReviewEntity)

    // --- Payout Ledger ---

    @Query("SELECT * FROM payout_ledger WHERE userId = :userId ORDER BY createdAtMillis DESC")
    fun observePayoutLedger(userId: String): Flow<List<PayoutLedgerEntity>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertPayout(entity: PayoutLedgerEntity)
}

@Database(
    entities = [
        UserProfileEntity::class,
        ProduceListingEntity::class,
        OrderEntity::class,
        FarmerReviewEntity::class,
        PayoutLedgerEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class KisanDirectDatabase : RoomDatabase() {
    abstract fun dao(): KisanDirectDao

    companion object {
        @Volatile
        private var INSTANCE: KisanDirectDatabase? = null

        fun getInstance(context: Context): KisanDirectDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    KisanDirectDatabase::class.java,
                    "kisandirect_local.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}

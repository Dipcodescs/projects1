const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const FARMER_UID = "farmer_ravi_101";
const CONSUMER_UID = "consumer_ananya_202";
const DRIVER_UID = "driver_vikram_303";
const OTHER_UID = "other_user_404";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function pastDate(offsetMs = 5000) {
  return new Date(Date.now() - offsetMs);
}

function futureDate(offsetMs = 60000) {
  return new Date(Date.now() + offsetMs);
}

function makePrivateUser(uid, role, overrides = {}) {
  const ts = pastDate(10000);
  return {
    userId: uid,
    displayName: "Test User",
    role,
    phone: "+919876543210",
    kycStatus: "VERIFIED",
    farmName: role === "FARMER" ? "Green Valley Farm" : "",
    city: "Nashik",
    defaultAddress: "Plot 12, Market Road, Nashik",
    latitude: 19.9975,
    longitude: 73.7898,
    languageCode: "en",
    vehicleType: role === "DRIVER" ? "Mini Truck" : "",
    vehiclePlate: role === "DRIVER" ? "MH15AB1234" : "",
    driverActive: role === "DRIVER",
    createdAt: ts,
    updatedAt: ts,
    ...overrides,
  };
}

function makePublicProfile(uid, role, overrides = {}) {
  const ts = pastDate(10000);
  return {
    userId: uid,
    displayName: role === "FARMER" ? "Ravi Patil" : "User",
    role,
    farmName: role === "FARMER" ? "Green Valley Farm" : "",
    city: "Nashik",
    latitude: 19.9975,
    longitude: 73.7898,
    kycVerified: true,
    averageRating: 0.0,
    ratingSum: 0,
    reviewCount: 0,
    completedDeliveries: 0,
    vehicleType: role === "DRIVER" ? "Mini Truck" : "",
    visibility: "PUBLIC",
    createdAt: ts,
    updatedAt: ts,
    ...overrides,
  };
}

function makeListing(farmerId = FARMER_UID, overrides = {}) {
  const ts = pastDate(10000);
  return {
    farmerId,
    farmerName: "Ravi Patil",
    farmName: "Green Valley Farm",
    cropName: "Heirloom Tomatoes",
    category: "VEGETABLES",
    pricePerKg: 38.0,
    marketComparePricePerKg: 62.0,
    availableQtyKg: 50.0,
    harvestDate: "2026-11-17",
    description: "Morning harvested vine-ripened tomatoes.",
    city: "Nashik",
    latitude: 19.9975,
    longitude: 73.7898,
    status: "ACTIVE",
    createdAt: ts,
    updatedAt: ts,
    ...overrides,
  };
}

function makeOrder(overrides = {}) {
  const ts = pastDate(10000);
  return {
    consumerId: CONSUMER_UID,
    consumerName: "Ananya Deshmukh",
    farmerId: FARMER_UID,
    farmerName: "Ravi Patil",
    farmName: "Green Valley Farm",
    driverId: "",
    driverName: "",
    items: [
      {
        listingId: "listing_tomatoes_1",
        cropName: "Heirloom Tomatoes",
        quantityKg: 5.0,
        pricePerKg: 38.0,
        lineTotal: 190.0,
        farmerPayoutAmount: 180.5,
      },
    ],
    totalWeightKg: 5.0,
    subtotalAmount: 190.0,
    farmerPayoutAmount: 180.5,
    driverPayoutAmount: 35.0,
    platformFeeAmount: 9.5,
    totalAmount: 225.0,
    paymentStatus: "PAID_SPLIT_SETTLED",
    paymentRef: "SPLIT_1794988001",
    deliveryStatus: "READY_FOR_PICKUP",
    pickupAddress: "Green Valley Farm, Nashik",
    pickupLat: 19.9975,
    pickupLng: 73.7898,
    deliveryAddress: "Flat 402, College Road, Nashik",
    deliveryLat: 20.0059,
    deliveryLng: 73.7609,
    distanceKm: 3.2,
    reviewed: false,
    createdAt: ts,
    updatedAt: ts,
    ...overrides,
  };
}

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated user is denied across all collections", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(FARMER_UID).get());
  await assertFails(unauthDb.collection("public_profiles").where("visibility", "==", "PUBLIC").get());
  await assertFails(unauthDb.collection("produce_listings").where("status", "==", "ACTIVE").get());
  await assertFails(unauthDb.collection("orders").where("consumerId", "==", CONSUMER_UID).get());
  await assertFails(unauthDb.collection("reviews").where("verifiedPurchase", "==", true).get());
});

test("PII Isolation: private user profiles and payouts are strictly owner-only", async () => {
  const farmerDb = testEnv.authenticatedContext(FARMER_UID).firestore();
  const otherDb = testEnv.authenticatedContext(OTHER_UID).firestore();

  // First-time user onboarding check: owner can safely get non-existent private/public docs
  await assertSucceeds(farmerDb.collection("users").doc(FARMER_UID).get());
  await assertSucceeds(farmerDb.collection("public_profiles").doc(FARMER_UID).get());

  await assertSucceeds(
    farmerDb.collection("users").doc(FARMER_UID).set(makePrivateUser(FARMER_UID, "FARMER"))
  );
  await assertSucceeds(farmerDb.collection("users").doc(FARMER_UID).get());

  // Non-owner cannot read or modify PII or list users
  await assertFails(otherDb.collection("users").doc(FARMER_UID).get());
  await assertFails(otherDb.collection("users").get());
  await assertFails(
    otherDb.collection("users").doc(FARMER_UID).update({
      phone: "+910000000000",
      updatedAt: pastDate(1000),
    })
  );

  // Reject Ghost Field / Shadow Update and future timestamp
  await assertFails(
    farmerDb.collection("users").doc(FARMER_UID).update({
      isAdmin: true,
      updatedAt: pastDate(1000),
    })
  );
  await assertFails(
    farmerDb.collection("users").doc(FARMER_UID).update({
      city: "Pune",
      updatedAt: futureDate(),
    })
  );

  // Payout subcollection owner-only
  await assertSucceeds(
    farmerDb.collection("users").doc(FARMER_UID).collection("payouts").doc("pay_1").set({
      orderId: "ord_1",
      entryType: "FARMER_SETTLEMENT",
      amount: 180.5,
      status: "CREDITED",
      note: "Direct farm-gate settlement",
      createdAt: pastDate(1000),
    })
  );
  await assertSucceeds(farmerDb.collection("users").doc(FARMER_UID).collection("payouts").get());
  await assertFails(otherDb.collection("users").doc(FARMER_UID).collection("payouts").get());
});

test("RBAC on produce_listings: only Farmers create/edit prices; Consumers only decrement stock", async () => {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const adminDb = ctx.firestore();
    await adminDb.collection("users").doc(FARMER_UID).set(makePrivateUser(FARMER_UID, "FARMER"));
    await adminDb.collection("users").doc(CONSUMER_UID).set(makePrivateUser(CONSUMER_UID, "CONSUMER"));
  });

  const farmerDb = testEnv.authenticatedContext(FARMER_UID).firestore();
  const consumerDb = testEnv.authenticatedContext(CONSUMER_UID).firestore();

  // Consumer cannot create a listing
  await assertFails(
    consumerDb.collection("produce_listings").doc("list_bad").set(makeListing(CONSUMER_UID))
  );

  // Farmer creates valid listing (no @DocumentId `id` field)
  await assertSucceeds(
    farmerDb.collection("produce_listings").doc("list_1").set(makeListing(FARMER_UID))
  );

  // Reject listing with `@DocumentId` 'id' field leaked in map
  await assertFails(
    farmerDb.collection("produce_listings").doc("list_2").set(makeListing(FARMER_UID, { id: "list_2" }))
  );

  // Filtered Kotlin queries succeed; unfiltered collection scan fails
  await assertSucceeds(consumerDb.collection("produce_listings").where("status", "==", "ACTIVE").get());
  await assertSucceeds(farmerDb.collection("produce_listings").where("farmerId", "==", FARMER_UID).get());
  await assertFails(consumerDb.collection("produce_listings").get());

  // Consumer CANNOT tamper with pricePerKg or increase stock
  await assertFails(
    consumerDb.collection("produce_listings").doc("list_1").update({
      pricePerKg: 1.0,
      updatedAt: pastDate(1000),
    })
  );
  await assertFails(
    consumerDb.collection("produce_listings").doc("list_1").update({
      availableQtyKg: 60.0,
      status: "ACTIVE",
      updatedAt: pastDate(1000),
    })
  );

  // Consumer CAN atomically decrement availableQtyKg during checkout
  await assertSucceeds(
    consumerDb.collection("produce_listings").doc("list_1").update({
      availableQtyKg: 45.0,
      status: "ACTIVE",
      updatedAt: pastDate(1000),
    })
  );

  // Farmer CAN edit their own listing price
  await assertSucceeds(
    farmerDb.collection("produce_listings").doc("list_1").update({
      pricePerKg: 42.0,
      updatedAt: pastDate(500),
    })
  );
});

test("Driver workflow enforces strict forward state machine and RBAC", async () => {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const adminDb = ctx.firestore();
    await adminDb.collection("users").doc(FARMER_UID).set(makePrivateUser(FARMER_UID, "FARMER"));
    await adminDb.collection("users").doc(CONSUMER_UID).set(makePrivateUser(CONSUMER_UID, "CONSUMER"));
    await adminDb.collection("users").doc(DRIVER_UID).set(makePrivateUser(DRIVER_UID, "DRIVER"));
    await adminDb.collection("public_profiles").doc(FARMER_UID).set(makePublicProfile(FARMER_UID, "FARMER"));
  });

  const consumerDb = testEnv.authenticatedContext(CONSUMER_UID).firestore();
  const driverDb = testEnv.authenticatedContext(DRIVER_UID).firestore();
  const otherDb = testEnv.authenticatedContext(OTHER_UID).firestore();

  // Consumer creates order
  await assertSucceeds(consumerDb.collection("orders").doc("ord_100").set(makeOrder()));

  // Filtered queries match security rules; unfiltered list fails
  await assertSucceeds(consumerDb.collection("orders").where("consumerId", "==", CONSUMER_UID).get());
  await assertSucceeds(
    driverDb.collection("orders")
      .where("deliveryStatus", "==", "READY_FOR_PICKUP")
      .where("driverId", "==", "")
      .get()
  );
  await assertFails(otherDb.collection("orders").get());

  // Consumer cannot claim delivery task
  await assertFails(
    consumerDb.collection("orders").doc("ord_100").update({
      driverId: CONSUMER_UID,
      driverName: "Ananya",
      deliveryStatus: "ASSIGNED",
      updatedAt: pastDate(2000),
    })
  );

  // Driver cannot shortcut directly from READY_FOR_PICKUP to DELIVERED
  await assertFails(
    driverDb.collection("orders").doc("ord_100").update({
      driverId: DRIVER_UID,
      driverName: "Vikram",
      deliveryStatus: "DELIVERED",
      deliveredAt: pastDate(2000),
      updatedAt: pastDate(2000),
    })
  );

  // Strict forward transitions succeed: READY_FOR_PICKUP -> ASSIGNED -> PICKED_UP -> EN_ROUTE -> DELIVERED
  await assertSucceeds(
    driverDb.collection("orders").doc("ord_100").update({
      driverId: DRIVER_UID,
      driverName: "Vikram Singh",
      deliveryStatus: "ASSIGNED",
      updatedAt: pastDate(4000),
    })
  );
  await assertSucceeds(
    driverDb.collection("orders").doc("ord_100").update({
      deliveryStatus: "PICKED_UP",
      updatedAt: pastDate(3000),
    })
  );
  await assertSucceeds(
    driverDb.collection("orders").doc("ord_100").update({
      deliveryStatus: "EN_ROUTE",
      updatedAt: pastDate(2000),
    })
  );
  await assertSucceeds(
    driverDb.collection("orders").doc("ord_100").update({
      deliveryStatus: "DELIVERED",
      deliveredAt: pastDate(1000),
      updatedAt: pastDate(1000),
    })
  );

  // Terminal State Lock: driver cannot revert status after DELIVERED
  await assertFails(
    driverDb.collection("orders").doc("ord_100").update({
      deliveryStatus: "EN_ROUTE",
      updatedAt: pastDate(500),
    })
  );
});

test("Verified Purchase Reviews: blocked before delivery, atomic rating sync after delivery, no double review", async () => {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const adminDb = ctx.firestore();
    await adminDb.collection("public_profiles").doc(FARMER_UID).set(makePublicProfile(FARMER_UID, "FARMER"));
    await adminDb.collection("orders").doc("ord_pending").set(makeOrder({ deliveryStatus: "EN_ROUTE" }));
    await adminDb.collection("orders").doc("ord_done").set(
      makeOrder({
        driverId: DRIVER_UID,
        driverName: "Vikram Singh",
        deliveryStatus: "DELIVERED",
        deliveredAt: pastDate(2000),
      })
    );
  });

  const consumerDb = testEnv.authenticatedContext(CONSUMER_UID).firestore();
  const farmerDb = testEnv.authenticatedContext(FARMER_UID).firestore();
  const otherDb = testEnv.authenticatedContext(OTHER_UID).firestore();

  // Farmer cannot forge own rating on public_profiles
  await assertFails(
    farmerDb.collection("public_profiles").doc(FARMER_UID).update({
      averageRating: 5.0,
      ratingSum: 50,
      reviewCount: 10,
      updatedAt: pastDate(1000),
    })
  );

  // Consumer cannot review an order that is NOT delivered yet
  await assertFails(
    consumerDb.collection("reviews").doc("ord_pending").set({
      orderId: "ord_pending",
      farmerId: FARMER_UID,
      farmerName: "Ravi Patil",
      consumerId: CONSUMER_UID,
      consumerName: "Ananya Deshmukh",
      rating: 5,
      comment: "Great tomatoes!",
      cropSummary: "Heirloom Tomatoes (5 kg)",
      verifiedPurchase: true,
      createdAt: pastDate(1000),
    })
  );

  // Unrelated user cannot review Consumer's delivered order
  await assertFails(
    otherDb.collection("reviews").doc("ord_done").set({
      orderId: "ord_done",
      farmerId: FARMER_UID,
      farmerName: "Ravi Patil",
      consumerId: OTHER_UID,
      consumerName: "Fake Reviewer",
      rating: 1,
      comment: "Fake review attempt",
      cropSummary: "Heirloom Tomatoes (5 kg)",
      verifiedPurchase: true,
      createdAt: pastDate(1000),
    })
  );

  // Atomic batch: Consumer creates verified review + marks order reviewed + updates farmer aggregate rating
  const batch = consumerDb.batch();
  batch.set(consumerDb.collection("reviews").doc("ord_done"), {
    orderId: "ord_done",
    farmerId: FARMER_UID,
    farmerName: "Ravi Patil",
    consumerId: CONSUMER_UID,
    consumerName: "Ananya Deshmukh",
    rating: 5,
    comment: "Crisp morning harvest, direct farm taste and fair price!",
    cropSummary: "Heirloom Tomatoes (5 kg)",
    verifiedPurchase: true,
    createdAt: pastDate(1000),
  });
  batch.update(consumerDb.collection("orders").doc("ord_done"), {
    reviewed: true,
    updatedAt: pastDate(1000),
  });
  batch.update(consumerDb.collection("public_profiles").doc(FARMER_UID), {
    ratingSum: 5,
    reviewCount: 1,
    averageRating: 5.0,
    lastReviewedOrderId: "ord_done",
    updatedAt: pastDate(1000),
  });
  await assertSucceeds(batch.commit());

  // Public query for verified farmer reviews succeeds
  await assertSucceeds(
    consumerDb.collection("reviews")
      .where("farmerId", "==", FARMER_UID)
      .where("verifiedPurchase", "==", true)
      .get()
  );

  // Cannot update or delete review after submission
  await assertFails(
    consumerDb.collection("reviews").doc("ord_done").update({
      rating: 1,
    })
  );
  await assertFails(consumerDb.collection("reviews").doc("ord_done").delete());
});

const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function serverTimestamp() {
  return new Date(Date.now() - 1000);
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

// --- 1. Unauthenticated access must fail ---
test("Unauthenticated user cannot read or write user profile, cart, orders, or favorites", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("cartItems").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("orders").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("favorites").get());
});

// --- 2. User Profile rules ---
test("Authenticated user can create, read, and update own profile", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const profileRef = aliceDb.collection("users").doc(ALICE_UID);

  await assertSucceeds(
    profileRef.set({
      userId: ALICE_UID,
      displayName: "Aarav Rawat",
      email: "aarav@example.com",
      deliveryAddress: "Mall Road, Almora",
      preferredRegion: "Kumaon",
      createdAt: serverTimestamp(),
      updatedAt: serverTimestamp(),
    })
  );

  await assertSucceeds(profileRef.get());

  await assertSucceeds(
    profileRef.update({
      deliveryAddress: "Nainital Ridge, Uttarakhand",
      updatedAt: serverTimestamp(),
    })
  );
});

test("Shadow update with ghost field on user profile is rejected", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const profileRef = aliceDb.collection("users").doc(ALICE_UID);

  await assertFails(
    profileRef.set({
      userId: ALICE_UID,
      displayName: "Aarav Rawat",
      isAdmin: true,
      createdAt: serverTimestamp(),
      updatedAt: serverTimestamp(),
    })
  );
});

test("Cross-user access to profile is denied", async () => {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx.firestore().collection("users").doc(BOB_UID).set({
      userId: BOB_UID,
      displayName: "Bob",
      createdAt: serverTimestamp(),
      updatedAt: serverTimestamp(),
    });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(aliceDb.collection("users").doc(BOB_UID).get());
  await assertFails(
    aliceDb.collection("users").doc(BOB_UID).update({
      displayName: "Hacked",
      updatedAt: serverTimestamp(),
    })
  );
});

// --- 3. Cart Items rules ---
test("Owner can create, list, update quantity, and delete cart item", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const cartRef = aliceDb
    .collection("users")
    .doc(ALICE_UID)
    .collection("cartItems")
    .doc("wild_forest_honey");

  await assertSucceeds(
    cartRef.set({
      userId: ALICE_UID,
      productId: "wild_forest_honey",
      productName: "Wild Himalayan Forest Honey",
      category: "Honey & Ghee",
      unitLabel: "500g Jar",
      unitPrice: 650,
      quantity: 2,
      createdAt: serverTimestamp(),
      updatedAt: serverTimestamp(),
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("cartItems").get()
  );

  await assertSucceeds(
    cartRef.update({
      quantity: 3,
      updatedAt: serverTimestamp(),
    })
  );

  await assertSucceeds(cartRef.delete());
});

test("Cart item rejects invalid quantity or cross-user read", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const cartRef = aliceDb
    .collection("users")
    .doc(ALICE_UID)
    .collection("cartItems")
    .doc("bad_qty");

  await assertFails(
    cartRef.set({
      userId: ALICE_UID,
      productId: "wild_forest_honey",
      productName: "Wild Himalayan Forest Honey",
      category: "Honey & Ghee",
      unitLabel: "500g Jar",
      unitPrice: 650,
      quantity: 0,
      createdAt: serverTimestamp(),
      updatedAt: serverTimestamp(),
    })
  );

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("cartItems").get()
  );
});

// --- 4. Orders rules & terminal state locking ---
test("Owner can place order and cancel while placed, then locked", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const orderRef = aliceDb
    .collection("users")
    .doc(ALICE_UID)
    .collection("orders")
    .doc("order_001");

  await assertSucceeds(
    orderRef.set({
      userId: ALICE_UID,
      orderNumber: "PO-1001",
      itemSummary: "Wild Himalayan Forest Honey x2",
      itemCount: 2,
      totalAmount: 1300,
      deliveryAddress: "Mall Road, Almora",
      status: "placed",
      createdAt: serverTimestamp(),
      updatedAt: serverTimestamp(),
    })
  );

  // Cancel is allowed from 'placed'
  await assertSucceeds(
    orderRef.update({
      status: "cancelled",
      updatedAt: serverTimestamp(),
    })
  );

  // Terminal state locking: cannot modify after cancelled
  await assertFails(
    orderRef.update({
      status: "placed",
      updatedAt: serverTimestamp(),
    })
  );

  // Cannot delete orders
  await assertFails(orderRef.delete());
});

// --- 5. Favorites rules ---
test("Owner can add, list, and remove favorite organic harvest", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const favRef = aliceDb
    .collection("users")
    .doc(ALICE_UID)
    .collection("favorites")
    .doc("kumaoni_red_rice");

  await assertSucceeds(
    favRef.set({
      userId: ALICE_UID,
      productId: "kumaoni_red_rice",
      productName: "Kumaoni Red Rice",
      createdAt: serverTimestamp(),
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("favorites").get()
  );

  await assertSucceeds(favRef.delete());
});

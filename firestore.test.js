const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const CUSTOMER_UID = "customer_ramesh_123";
const OTHER_CUSTOMER_UID = "customer_suresh_456";
const ADMIN_UID = "admin_mahadev_789";
const OWNER_UID = "owner_cp_001";
const ADMIN_PASSCODE_HASH = "96ca83a9288cbced33d8415060768b931bd8400373792d4cadeb02d57d10803f";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

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

test("Unauthenticated user cannot read or write any collection", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("grains").get());
  await assertFails(unauthDb.collection("loginLogs").get());
  await assertFails(unauthDb.collection("users").doc(CUSTOMER_UID).get());
  await assertFails(
    unauthDb.collection("grains").doc("wheat").set({
      name: "ઘઉં",
      category: "અનાજ",
      pricePerKg: 32,
      priceUnit: "Kg",
      weight: "50 Kg (બેગ)",
      stockStatus: "ઉપલબ્ધ",
      iconKey: "wheat",
      updatedBy: CUSTOMER_UID,
      createdAt: new Date(Date.now() - 1000),
      updatedAt: new Date(Date.now() - 1000),
    })
  );
});

test("Customer can register own profile and log customer login, but cannot read other customers or list all loginLogs", async () => {
  const now = new Date(Date.now() - 2000);
  const customerDb = testEnv.authenticatedContext(CUSTOMER_UID, { email: "ramesh@example.com", email_verified: true }).firestore();
  const otherDb = testEnv.authenticatedContext(OTHER_CUSTOMER_UID, { email: "suresh@example.com", email_verified: true }).firestore();

  await assertSucceeds(
    customerDb.collection("users").doc(CUSTOMER_UID).set({
      userId: CUSTOMER_UID,
      mobileNumber: "9876543210",
      displayName: "રમેશભાઈ પટેલ",
      email: "ramesh@example.com",
      createdAt: now,
      updatedAt: now,
    })
  );

  // Customer can update own mobile/name
  await assertSucceeds(
    customerDb.collection("users").doc(CUSTOMER_UID).update({
      displayName: "રમેશભાઈ હળવદ",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Customer cannot inject ghost field or mutate createdAt
  await assertFails(
    customerDb.collection("users").doc(CUSTOMER_UID).update({
      isAdmin: true,
      updatedAt: new Date(Date.now() - 1000),
    })
  );
  await assertFails(
    customerDb.collection("users").doc(CUSTOMER_UID).update({
      createdAt: new Date(Date.now() - 500),
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Customer can log own customer login
  await assertSucceeds(
    customerDb.collection("loginLogs").doc("log_1").set({
      userId: CUSTOMER_UID,
      mobileNumber: "9876543210",
      customerName: "રમેશભાઈ હળવદ",
      email: "ramesh@example.com",
      loginRole: "customer",
      loginAt: new Date(Date.now() - 1000),
    })
  );

  // Customer cannot spoof admin role in loginLogs
  await assertFails(
    customerDb.collection("loginLogs").doc("log_spoof").set({
      userId: CUSTOMER_UID,
      mobileNumber: "9876543210",
      customerName: "Hacker",
      loginRole: "admin",
      loginAt: new Date(Date.now() - 1000),
    })
  );

  // Cross-user PII isolation
  await assertFails(otherDb.collection("users").doc(CUSTOMER_UID).get());
  await assertFails(otherDb.collection("users").get());
  await assertFails(otherDb.collection("loginLogs").get());
  await assertFails(otherDb.collection("loginLogs").doc("log_1").get());

  // Customer CAN query own loginLogs
  await assertSucceeds(
    customerDb.collection("loginLogs").where("userId", "==", CUSTOMER_UID).get()
  );
});

test("Customer cannot modify grains or promote self to admin with wrong credentials", async () => {
  const now = new Date(Date.now() - 2000);
  const customerDb = testEnv.authenticatedContext(CUSTOMER_UID).firestore();

  await assertFails(
    customerDb.collection("grains").doc("wheat").set({
      name: "ઘઉં",
      category: "અનાજ",
      pricePerKg: 32,
      priceUnit: "Kg",
      weight: "50 Kg (બેગ)",
      stockStatus: "ઉપલબ્ધ",
      iconKey: "wheat",
      updatedBy: CUSTOMER_UID,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Wrong mobile number rejected
  await assertFails(
    customerDb.collection("admins").doc(CUSTOMER_UID).set({
      userId: CUSTOMER_UID,
      mobileNumber: "9999999999",
      adminLabel: "Fake Admin",
      passcodeHash: ADMIN_PASSCODE_HASH,
      createdAt: now,
    })
  );

  // Wrong passcode hash rejected
  await assertFails(
    customerDb.collection("admins").doc(CUSTOMER_UID).set({
      userId: CUSTOMER_UID,
      mobileNumber: "9879730448",
      adminLabel: "Fake Admin",
      passcodeHash: "0000000000000000000000000000000000000000000000000000000000000000",
      createdAt: now,
    })
  );
});

test("Admin verified with authorized mobile + passcodeHash can manage grains and view all loginLogs", async () => {
  const now = new Date(Date.now() - 5000);
  const adminDb = testEnv.authenticatedContext(ADMIN_UID).firestore();
  const customerDb = testEnv.authenticatedContext(CUSTOMER_UID).firestore();

  // Admin registers with valid authorized mobile + SHA-256 passcode hash
  await assertSucceeds(
    adminDb.collection("admins").doc(ADMIN_UID).set({
      userId: ADMIN_UID,
      mobileNumber: "9879730448",
      adminLabel: "Admin 1 (9879730448)",
      passcodeHash: ADMIN_PASSCODE_HASH,
      createdAt: now,
    })
  );

  // Admin creates grain item (NO 'id' field because Kotlin uses @DocumentId)
  await assertSucceeds(
    adminDb.collection("grains").doc("wheat_01").set({
      name: "ઘઉં",
      category: "અનાજ",
      pricePerKg: 32.0,
      priceUnit: "Kg",
      weight: "50 Kg (બેગ)",
      stockStatus: "ઉપલબ્ધ",
      iconKey: "wheat",
      updatedBy: ADMIN_UID,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Reject payload containing 'id' field or future timestamp
  await assertFails(
    adminDb.collection("grains").doc("rice_01").set({
      id: "rice_01",
      name: "ચોખા",
      category: "અનાજ",
      pricePerKg: 58.0,
      priceUnit: "Kg",
      weight: "50 Kg (બેગ)",
      stockStatus: "ઉપલબ્ધ",
      iconKey: "rice",
      updatedBy: ADMIN_UID,
      createdAt: now,
      updatedAt: now,
    })
  );
  await assertFails(
    adminDb.collection("grains").doc("future_01").set({
      name: "બાજરી",
      category: "અનાજ",
      pricePerKg: 40.0,
      priceUnit: "Kg",
      weight: "50 Kg (બેગ)",
      stockStatus: "ઉપલબ્ધ",
      iconKey: "bajra",
      updatedBy: ADMIN_UID,
      createdAt: new Date(Date.now() + 60000),
      updatedAt: new Date(Date.now() + 60000),
    })
  );

  // Admin partial update (price, weight, stock)
  await assertSucceeds(
    adminDb.collection("grains").doc("wheat_01").update({
      pricePerKg: 35.0,
      stockStatus: "ઓછો સ્ટોક",
      updatedBy: ADMIN_UID,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Customer can read grains & add to own watchlist
  await assertSucceeds(customerDb.collection("grains").get());
  await assertSucceeds(
    customerDb.collection("users").doc(CUSTOMER_UID).collection("watchlist").doc("wheat_01").set({
      grainId: "wheat_01",
      addedAt: new Date(Date.now() - 1000),
    })
  );
  await assertSucceeds(
    customerDb.collection("users").doc(CUSTOMER_UID).collection("watchlist").get()
  );

  // Customer cannot delete grain
  await assertFails(customerDb.collection("grains").doc("wheat_01").delete());

  // Customer logs in, Admin can list all loginLogs
  await assertSucceeds(
    customerDb.collection("loginLogs").doc("cust_log_1").set({
      userId: CUSTOMER_UID,
      mobileNumber: "9876543210",
      customerName: "રમેશભાઈ",
      loginRole: "customer",
      loginAt: new Date(Date.now() - 1000),
    })
  );
  await assertSucceeds(adminDb.collection("loginLogs").get());

  // Admin can delete grain
  await assertSucceeds(adminDb.collection("grains").doc("wheat_01").delete());
});

test("Email spoofing test: unverified owner email is rejected from admin actions", async () => {
  const spoofedOwnerDb = testEnv
    .authenticatedContext(OWNER_UID, { email: "cp144604@gmail.com", email_verified: false })
    .firestore();
  const verifiedOwnerDb = testEnv
    .authenticatedContext(OWNER_UID, { email: "cp144604@gmail.com", email_verified: true })
    .firestore();

  const now = new Date(Date.now() - 1000);
  await assertFails(
    spoofedOwnerDb.collection("grains").doc("jowar_01").set({
      name: "જુવાર",
      category: "અનાજ",
      pricePerKg: 38.0,
      priceUnit: "Kg",
      weight: "50 Kg (બેગ)",
      stockStatus: "ઉપલબ્ધ",
      iconKey: "jowar",
      updatedBy: OWNER_UID,
      createdAt: now,
      updatedAt: now,
    })
  );
  await assertFails(spoofedOwnerDb.collection("loginLogs").get());

  await assertSucceeds(
    verifiedOwnerDb.collection("grains").doc("jowar_01").set({
      name: "જુવાર",
      category: "અનાજ",
      pricePerKg: 38.0,
      priceUnit: "Kg",
      weight: "50 Kg (બેગ)",
      stockStatus: "ઉપલબ્ધ",
      iconKey: "jowar",
      updatedBy: OWNER_UID,
      createdAt: now,
      updatedAt: now,
    })
  );
  await assertSucceeds(verifiedOwnerDb.collection("loginLogs").get());
});

test("Only admin can update storeConfig QR download URL; signed-in customers can read it", async () => {
  const ownerDb = testEnv
    .authenticatedContext(OWNER_UID, { email: "cp144604@gmail.com", email_verified: true })
    .firestore();
  const customerDb = testEnv.authenticatedContext(CUSTOMER_UID).firestore();
  const now = new Date(Date.now() - 1000);

  await assertFails(
    customerDb.collection("storeConfig").doc("appShare").set({
      downloadUrl: "https://evil.example.com/app.apk",
      headline: "Hacked",
      updatedBy: CUSTOMER_UID,
      updatedAt: now,
    })
  );

  await assertSucceeds(
    ownerDb.collection("storeConfig").doc("appShare").set({
      downloadUrl: "https://play.google.com/store/apps/details?id=com.aistudio.mahadevtraders.hkqmzp",
      headline: "મહાદેવ ટ્રેડર્સ અનાજ ભાવ એપ ડાઉનલોડ કરો",
      updatedBy: OWNER_UID,
      updatedAt: now,
    })
  );

  await assertSucceeds(customerDb.collection("storeConfig").doc("appShare").get());
});


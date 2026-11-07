// Initialises the Firebase Admin SDK once for the whole API.
//
// The Admin SDK gives this server three things (Google, 2026a):
//   1. Cloud Firestore  - the NoSQL database the API reads and writes
//   2. Firebase Auth    - verifying the ID token each Android request sends
//   3. Cloud Messaging  - sending real-time push notifications to devices
//
// Credentials come from an environment variable so the service-account key is
// never committed to GitHub:
//   FIREBASE_SERVICE_ACCOUNT         - the service-account JSON, pasted as-is
//   FIREBASE_SERVICE_ACCOUNT_BASE64  - the same JSON, base64-encoded (optional)
//   GOOGLE_APPLICATION_CREDENTIALS   - path to the JSON file (local development)
//
// When none are set (unit tests, CI, quick local runs) the API falls back to a
// local JSON file store and skips push notifications, so it still works offline.
//
// Reference: Google (2026a) Add the Firebase Admin SDK to your server.
// https://firebase.google.com/docs/admin/setup
const admin = require("firebase-admin");

let initialised = false;
let available = false;

function readServiceAccount() {
  if (process.env.FIREBASE_SERVICE_ACCOUNT) {
    return JSON.parse(process.env.FIREBASE_SERVICE_ACCOUNT);
  }
  if (process.env.FIREBASE_SERVICE_ACCOUNT_BASE64) {
    const json = Buffer.from(process.env.FIREBASE_SERVICE_ACCOUNT_BASE64, "base64").toString("utf8");
    return JSON.parse(json);
  }
  return null;
}

/** Returns true when the Admin SDK is configured and ready to use. */
function init() {
  if (initialised) return available;
  initialised = true;

  try {
    const serviceAccount = readServiceAccount();
    if (serviceAccount) {
      admin.initializeApp({ credential: admin.credential.cert(serviceAccount) });
      available = true;
    } else if (process.env.GOOGLE_APPLICATION_CREDENTIALS) {
      admin.initializeApp();
      available = true;
    }
  } catch (err) {
    console.error("[firebase] Could not initialise the Admin SDK:", err.message);
    available = false;
  }

  console.log(
    available
      ? "[firebase] Admin SDK ready - using Cloud Firestore, token verification and FCM"
      : "[firebase] No service account configured - using local JSON store, push disabled"
  );
  return available;
}

function isAvailable() {
  return init();
}

module.exports = { admin, init, isAvailable };

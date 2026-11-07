// REST API for the Student Assistant Android app (OPSC6312 Final PoE).
//
//   Android app --HTTPS + Firebase ID token--> this Express API --> Cloud Firestore (NoSQL)
//                                                     |
//                                                     +--> Firebase Cloud Messaging (push)
//
// Authentication: every /api request must carry "Authorization: Bearer <Firebase
// ID token>". The token is verified with the Firebase Admin SDK, and the user's
// UID inside it scopes every read and write, so one student can never read or
// change another student's data. (Part 2 trusted a plain X-User-Id header; that
// is now only accepted when the Admin SDK is not configured, i.e. local tests.)
//
// Reference: Google (2026e) Verify ID tokens.
// https://firebase.google.com/docs/auth/admin/verify-id-tokens
const express = require("express");
const cors = require("cors");

const firebase = require("./firebase");
const store = require("./store");
const notify = require("./notify");

const app = express();
const PORT = process.env.PORT || 3000;

app.use(cors());
app.use(express.json({ limit: "100kb" }));

// Request log - visible in Render's "Logs" tab during the demonstration.
app.use((req, res, next) => {
  const started = Date.now();
  res.on("finish", () => {
    console.log(`${new Date().toISOString()} ${req.method} ${req.originalUrl} -> ${res.statusCode} (${Date.now() - started}ms)`);
  });
  next();
});

// Health check, used by Render and handy for showing the live API in a browser.
app.get("/", (req, res) => {
  res.json({
    status: "ok",
    service: "student-assistant-api",
    version: "3.0.0",
    database: store.driverName(),
    pushNotifications: firebase.isAvailable()
  });
});

/** Identifies the caller from their Firebase ID token (or X-User-Id in local test mode). */
async function authenticate(req, res, next) {
  const header = req.header("Authorization") || "";
  const token = header.startsWith("Bearer ") ? header.slice(7) : null;

  if (firebase.isAvailable() && process.env.ALLOW_HEADER_AUTH !== "true") {
    if (!token) return res.status(401).json({ error: "Missing Firebase ID token" });
    try {
      const decoded = await firebase.admin.auth().verifyIdToken(token);
      req.userId = decoded.uid;
      return next();
    } catch (err) {
      console.warn("[auth] Rejected token:", err.code || err.message);
      return res.status(401).json({ error: "Invalid or expired Firebase ID token" });
    }
  }

  // Local / CI mode with no Firebase credentials: trust the X-User-Id header.
  const userId = req.header("X-User-Id");
  if (!userId) return res.status(401).json({ error: "Missing X-User-Id header" });
  req.userId = userId;
  next();
}

app.use("/api", authenticate);
app.use("/api/modules", require("./routes/modules"));
app.use("/api/tasks", require("./routes/tasks"));
app.use("/api/timetable", require("./routes/timetable"));
app.use("/api/exams", require("./routes/exams"));
app.use("/api/study-sessions", require("./routes/studySessions"));
app.use("/api", require("./routes/account"));

app.use((req, res) => res.status(404).json({ error: "Not found" }));

// Central error handler: log the detail, return a clean JSON error to the app.
// eslint-disable-next-line no-unused-vars
app.use((err, req, res, next) => {
  if (err.type === "entity.parse.failed") return res.status(400).json({ error: "Body must be valid JSON" });
  console.error(`[error] ${req.method} ${req.originalUrl}:`, err);
  res.status(500).json({ error: "Internal server error" });
});

/**
 * Daily deadline reminders. Checked every 30 minutes; runs once per day after
 * 07:00 South African time for every user with an enabled device. The last run
 * date is stored in the database so a restart does not send duplicates.
 * (On Render's free tier the service sleeps when idle, so the app also schedules
 * local reminders with WorkManager - see ReminderWorker.kt.)
 */
async function dailyReminderTick() {
  try {
    const today = notify.isoDate(0);
    const saHour = (new Date().getUTCHours() + 2) % 24;
    if (saHour < 7) return;
    const meta = (await store.getDoc("meta", "reminders")) || {};
    if (meta.lastRun === today) return;
    await store.setDoc("meta", "reminders", { lastRun: today });

    const devices = await store.findByField("devices", "enabled", true);
    const users = [...new Set(devices.map((d) => d.userId))];
    for (const userId of users) await notify.sendRemindersForUser(userId);
    console.log(`[reminders] Daily reminders sent to ${users.length} user(s)`);
  } catch (err) {
    console.error("[reminders] Daily job failed:", err.message);
  }
}

if (require.main === module) {
  app.listen(PORT, () => {
    console.log(`Student Assistant API listening on port ${PORT} (database: ${store.driverName()})`);
    setInterval(dailyReminderTick, 30 * 60 * 1000);
    dailyReminderTick();
  });
}

module.exports = app;

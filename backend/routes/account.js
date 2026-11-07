// Account-level endpoints that are not plain CRUD collections:
//
//   GET  /api/profile          - the user's profile + settings document
//   PUT  /api/profile          - save name, language, theme, notification flag, photo URL
//   POST /api/devices          - register / refresh this device's FCM token
//   DELETE /api/devices/:token - unregister on sign-out
//   POST /api/notifications/test      - send a test push to all my devices
//   POST /api/notifications/reminders - send my due-soon reminders right now
//
// Settings are stored in Firestore (users/{uid}) so they follow the user to any
// device they sign in on, rather than only living in SharedPreferences.
const express = require("express");
const store = require("../store");
const notify = require("../notify");
const { isBlank, trim } = require("./crud");

const router = express.Router();
const wrap = (fn) => (req, res, next) => Promise.resolve(fn(req, res, next)).catch(next);

const LANGUAGES = ["en", "nso", "zu"];
const DEFAULT_PROFILE = { displayName: "", language: "en", darkTheme: false, notificationsEnabled: true, photoUrl: "" };

router.get(
  "/profile",
  wrap(async (req, res) => {
    const profile = await store.getDoc("users", req.userId);
    res.json({ ...DEFAULT_PROFILE, ...(profile || {}), userId: req.userId });
  })
);

router.put(
  "/profile",
  wrap(async (req, res) => {
    const b = req.body || {};
    if (b.language !== undefined && !LANGUAGES.includes(b.language)) {
      return res.status(400).json({ error: "language must be en, nso or zu" });
    }
    const fields = { userId: req.userId, updatedAt: new Date().toISOString() };
    if (b.displayName !== undefined) fields.displayName = trim(b.displayName).slice(0, 80);
    if (b.email !== undefined) fields.email = trim(b.email);
    if (b.language !== undefined) fields.language = b.language;
    if (b.darkTheme !== undefined) fields.darkTheme = !!b.darkTheme;
    if (b.notificationsEnabled !== undefined) fields.notificationsEnabled = !!b.notificationsEnabled;
    if (b.photoUrl !== undefined) {
      const url = trim(b.photoUrl);
      if (url && !/^https:\/\//.test(url)) return res.status(400).json({ error: "photoUrl must be https" });
      fields.photoUrl = url;
    }

    const saved = await store.setDoc("users", req.userId, fields);

    // Keep the push preference on every device record in step with the profile.
    if (b.notificationsEnabled !== undefined) {
      const devices = await store.getAll("devices", req.userId);
      await Promise.all(devices.map((d) => store.update("devices", d.id, req.userId, { enabled: !!b.notificationsEnabled })));
    }
    console.log(`[profile] saved for ${req.userId}`);
    res.json({ ...DEFAULT_PROFILE, ...saved });
  })
);

router.post(
  "/devices",
  wrap(async (req, res) => {
    const { token, platform } = req.body || {};
    if (isBlank(token)) return res.status(400).json({ error: "token is required" });

    const profile = (await store.getDoc("users", req.userId)) || DEFAULT_PROFILE;
    const enabled = profile.notificationsEnabled !== false;

    // A token belongs to one user at a time: remove it from anyone else first
    // (e.g. two students sharing a phone and switching accounts).
    const owners = await store.findByField("devices", "token", token);
    for (const d of owners) {
      if (d.userId !== req.userId) await store.remove("devices", d.id, d.userId);
    }

    const mine = owners.find((d) => d.userId === req.userId);
    const row = mine
      ? await store.update("devices", mine.id, req.userId, { token, platform: platform || "android", enabled })
      : await store.insert("devices", req.userId, { token, platform: platform || "android", enabled });
    console.log(`[devices] registered device #${row.id} for ${req.userId}`);
    res.status(mine ? 200 : 201).json(row);
  })
);

router.delete(
  "/devices/:token",
  wrap(async (req, res) => {
    const owned = (await store.findByField("devices", "token", req.params.token)).filter((d) => d.userId === req.userId);
    for (const d of owned) await store.remove("devices", d.id, req.userId);
    res.status(204).send();
  })
);

router.post(
  "/notifications/test",
  wrap(async (req, res) => {
    const result = await notify.sendToUser(
      req.userId,
      "Student Assistant",
      "Push notifications are working - you'll be alerted about deadlines and exams.",
      { type: "test" }
    );
    res.json(result);
  })
);

router.post(
  "/notifications/reminders",
  wrap(async (req, res) => {
    const count = await notify.sendRemindersForUser(req.userId);
    res.json({ reminders: count });
  })
);

module.exports = router;

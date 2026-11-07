// Real-time push notifications via Firebase Cloud Messaging (FCM).
//
// Each Android device registers its FCM registration token with
// POST /api/devices. When something happens on the server (a task or exam is
// saved, a deadline is approaching) the API calls sendToUser(), which looks up
// every enabled device for that user and sends one multicast message.
// Tokens FCM reports as invalid (app uninstalled, token rotated) are deleted so
// they are not retried.
//
// Reference: Google (2026d) Send messages to multiple devices.
// https://firebase.google.com/docs/cloud-messaging/send-message
const firebase = require("./firebase");
const store = require("./store");

const INVALID_TOKEN_CODES = new Set([
  "messaging/registration-token-not-registered",
  "messaging/invalid-registration-token",
  "messaging/invalid-argument"
]);

/**
 * Sends a push notification to every device the user has registered.
 * Returns { sent, failed } so callers and tests can check the outcome.
 * Never throws: a notification failure must not fail the REST request.
 */
async function sendToUser(userId, title, body, data = {}) {
  try {
    const devices = (await store.getAll("devices", userId)).filter((d) => d.enabled !== false);
    if (devices.length === 0) {
      console.log(`[notify] ${userId} has no enabled devices - skipped "${title}"`);
      return { sent: 0, failed: 0 };
    }
    if (!firebase.isAvailable()) {
      console.log(`[notify] FCM not configured - would have sent "${title}" to ${devices.length} device(s)`);
      return { sent: 0, failed: 0 };
    }

    // Every value in an FCM data payload must be a string.
    const stringData = Object.fromEntries(Object.entries({ title, body, ...data }).map(([k, v]) => [k, String(v)]));

    const response = await firebase.admin.messaging().sendEachForMulticast({
      tokens: devices.map((d) => d.token),
      notification: { title, body },
      data: stringData,
      android: {
        priority: "high",
        notification: { channelId: "student_assistant_updates", icon: "ic_notification", color: "#2952E3" }
      }
    });

    // Remove tokens that FCM says will never work again.
    await Promise.all(
      response.responses.map(async (result, index) => {
        if (!result.success && INVALID_TOKEN_CODES.has(result.error && result.error.code)) {
          console.log(`[notify] Removing invalid token for device #${devices[index].id}`);
          await store.remove("devices", devices[index].id, userId);
        }
      })
    );

    console.log(`[notify] "${title}" -> ${response.successCount} sent, ${response.failureCount} failed`);
    return { sent: response.successCount, failed: response.failureCount };
  } catch (err) {
    console.error("[notify] Push failed:", err.message);
    return { sent: 0, failed: 1 };
  }
}

/** yyyy-MM-dd for today + offsetDays, in South African time (UTC+2, no DST). */
function isoDate(offsetDays = 0) {
  const date = new Date(Date.now() + 2 * 60 * 60 * 1000 + offsetDays * 24 * 60 * 60 * 1000);
  return date.toISOString().slice(0, 10);
}

/**
 * Builds the deadline reminders for one user: tasks due today or tomorrow and
 * exams in the next three days. Pure data in, list out, so it is unit-tested.
 */
function buildReminders(tasks, exams, today = isoDate(0)) {
  const tomorrow = new Date(`${today}T00:00:00Z`);
  tomorrow.setUTCDate(tomorrow.getUTCDate() + 1);
  const tomorrowIso = tomorrow.toISOString().slice(0, 10);
  const examHorizon = new Date(`${today}T00:00:00Z`);
  examHorizon.setUTCDate(examHorizon.getUTCDate() + 3);
  const horizonIso = examHorizon.toISOString().slice(0, 10);

  const reminders = [];
  tasks
    .filter((t) => !t.completed && (t.dueDate === today || t.dueDate === tomorrowIso))
    .forEach((t) =>
      reminders.push({
        title: t.dueDate === today ? "Due today" : "Due tomorrow",
        body: `${t.title} (${t.priority} priority)`,
        data: { type: "task", id: t.id }
      })
    );
  exams
    .filter((e) => e.date >= today && e.date <= horizonIso)
    .forEach((e) =>
      reminders.push({
        title: e.date === today ? "Exam today" : "Upcoming exam",
        body: `${e.title} on ${e.date}${e.time ? " at " + e.time : ""}`,
        data: { type: "exam", id: e.id }
      })
    );
  return reminders;
}

/** Sends all of one user's reminders. Used by the hourly job and the demo endpoint. */
async function sendRemindersForUser(userId) {
  const [tasks, exams] = await Promise.all([store.getAll("tasks", userId), store.getAll("exams", userId)]);
  const reminders = buildReminders(tasks, exams);
  for (const r of reminders) {
    await sendToUser(userId, r.title, r.body, r.data);
  }
  return reminders.length;
}

module.exports = { sendToUser, buildReminders, sendRemindersForUser, isoDate };

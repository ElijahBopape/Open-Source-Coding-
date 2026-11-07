// Automated API tests, run with `npm test` (Node's built-in test runner) locally
// and in GitHub Actions. They start the real Express app against a temporary
// JSON store, so no Firebase credentials or network access are needed.
//
// Reference: OpenJS Foundation (2026) Node.js test runner. https://nodejs.org/api/test.html
const { test, before, after } = require("node:test");
const assert = require("node:assert/strict");
const fs = require("fs");
const os = require("os");
const path = require("path");

const tmpFile = path.join(os.tmpdir(), `sa-test-${process.pid}.json`);
process.env.STORE = "json";
process.env.DATA_FILE = tmpFile;
delete process.env.FIREBASE_SERVICE_ACCOUNT;
delete process.env.GOOGLE_APPLICATION_CREDENTIALS;

const app = require("../server");
const { buildReminders } = require("../notify");

let server;
let baseUrl;

before(async () => {
  server = app.listen(0);
  await new Promise((resolve) => server.once("listening", resolve));
  baseUrl = `http://127.0.0.1:${server.address().port}`;
});

after(() => {
  server.close();
  fs.rmSync(tmpFile, { force: true });
});

async function call(method, url, body, user = "student-a") {
  const res = await fetch(baseUrl + url, {
    method,
    headers: { "Content-Type": "application/json", ...(user ? { "X-User-Id": user } : {}) },
    body: body ? JSON.stringify(body) : undefined
  });
  const text = await res.text();
  return { status: res.status, body: text ? JSON.parse(text) : null };
}

test("health check reports the store in use", async () => {
  const res = await call("GET", "/", null, null);
  assert.equal(res.status, 200);
  assert.equal(res.body.status, "ok");
  assert.equal(res.body.database, "json-file");
});

test("requests without a user are rejected with 401", async () => {
  const res = await call("GET", "/api/tasks", null, null);
  assert.equal(res.status, 401);
});

test("module CRUD round trip", async () => {
  const created = await call("POST", "/api/modules", { name: "Open Source Coding", code: "opsc6312" });
  assert.equal(created.status, 201);
  assert.equal(created.body.code, "OPSC6312");

  const updated = await call("PUT", `/api/modules/${created.body.id}`, { name: "OSC", code: "OPSC6312", lecturer: "Mr X" });
  assert.equal(updated.status, 200);
  assert.equal(updated.body.lecturer, "Mr X");

  const list = await call("GET", "/api/modules");
  assert.equal(list.body.length, 1);

  const removed = await call("DELETE", `/api/modules/${created.body.id}`);
  assert.equal(removed.status, 204);
});

test("task validation rejects bad input instead of storing it", async () => {
  assert.equal((await call("POST", "/api/tasks", { title: "", dueDate: "2026-11-10" })).status, 400);
  assert.equal((await call("POST", "/api/tasks", { title: "Essay", dueDate: "10/11/2026" })).status, 400);
  assert.equal((await call("POST", "/api/tasks", { title: "Essay", dueDate: "2026-11-10", priority: "Urgent" })).status, 400);
});

test("users cannot see or change each other's data", async () => {
  const mine = await call("POST", "/api/tasks", { title: "Private", dueDate: "2026-11-10" }, "student-a");
  assert.equal(mine.status, 201);

  const theirs = await call("GET", "/api/tasks", null, "student-b");
  assert.equal(theirs.body.length, 0);
  assert.equal((await call("PUT", `/api/tasks/${mine.body.id}`, { title: "Hacked", dueDate: "2026-11-10" }, "student-b")).status, 404);
  assert.equal((await call("DELETE", `/api/tasks/${mine.body.id}`, null, "student-b")).status, 404);
});

test("exams store an optional mark and validate its range", async () => {
  assert.equal((await call("POST", "/api/exams", { title: "Exam", date: "2026-11-20", mark: 140 })).status, 400);
  const exam = await call("POST", "/api/exams", { title: "OPSC6312 Exam", date: "2026-11-20", time: "09:00", mark: 74 });
  assert.equal(exam.status, 201);
  assert.equal(exam.body.mark, 74);
  assert.equal(exam.body.durationMinutes, 120);
});

test("study sessions require a positive duration", async () => {
  assert.equal((await call("POST", "/api/study-sessions", { title: "Revise", date: "2026-11-12", durationMinutes: 0 })).status, 400);
  const s = await call("POST", "/api/study-sessions", { title: "Revise ch.1", date: "2026-11-12", durationMinutes: 45, examId: 1 });
  assert.equal(s.status, 201);
  assert.equal(s.body.examId, 1);
});

test("profile settings are saved and language is validated", async () => {
  assert.equal((await call("PUT", "/api/profile", { language: "fr" })).status, 400);
  const saved = await call("PUT", "/api/profile", { displayName: "Elijah", language: "zu", darkTheme: true });
  assert.equal(saved.status, 200);
  const profile = await call("GET", "/api/profile");
  assert.equal(profile.body.language, "zu");
  assert.equal(profile.body.darkTheme, true);
});

test("device registration is idempotent per token", async () => {
  const first = await call("POST", "/api/devices", { token: "tok-1" });
  const second = await call("POST", "/api/devices", { token: "tok-1" });
  assert.equal(first.status, 201);
  assert.equal(second.status, 200);
  assert.equal(first.body.id, second.body.id);
});

test("buildReminders picks tasks due today/tomorrow and exams within 3 days", () => {
  const today = "2026-11-07";
  const tasks = [
    { id: 1, title: "Due today", dueDate: "2026-11-07", priority: "High", completed: false },
    { id: 2, title: "Due tomorrow", dueDate: "2026-11-08", priority: "Low", completed: false },
    { id: 3, title: "Done", dueDate: "2026-11-07", priority: "High", completed: true },
    { id: 4, title: "Next week", dueDate: "2026-11-14", priority: "High", completed: false }
  ];
  const exams = [
    { id: 1, title: "Soon", date: "2026-11-10", time: "09:00" },
    { id: 2, title: "Later", date: "2026-11-30", time: "" }
  ];
  const reminders = buildReminders(tasks, exams, today);
  assert.deepEqual(reminders.map((r) => r.title), ["Due today", "Due tomorrow", "Upcoming exam"]);
});

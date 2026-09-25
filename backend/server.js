// REST API for the Student Assistant Android app.
// Every request must carry an "X-User-Id" header (the caller's Firebase UID);
// all data is scoped to that user so different students never see each other's
// modules, tasks or timetable entries.
const express = require("express");
const cors = require("cors");

const modulesRouter = require("./routes/modules");
const tasksRouter = require("./routes/tasks");
const timetableRouter = require("./routes/timetable");

const app = express();
const PORT = process.env.PORT || 3000;

app.use(cors());
app.use(express.json());

// Simple request log so activity is visible in Render's log viewer during the demo.
app.use((req, res, next) => {
  console.log(`${new Date().toISOString()} ${req.method} ${req.path}`);
  next();
});

// Requires every /api/* request to identify the calling user.
app.use("/api", (req, res, next) => {
  const userId = req.header("X-User-Id");
  if (!userId) {
    return res.status(401).json({ error: "Missing X-User-Id header" });
  }
  req.userId = userId;
  next();
});

app.use("/api/modules", modulesRouter);
app.use("/api/tasks", tasksRouter);
app.use("/api/timetable", timetableRouter);

// Health check used by Render and for a quick manual sanity check in a browser.
app.get("/", (req, res) => {
  res.json({ status: "ok", service: "student-assistant-api" });
});

app.listen(PORT, () => {
  console.log(`Student Assistant API listening on port ${PORT}`);
});

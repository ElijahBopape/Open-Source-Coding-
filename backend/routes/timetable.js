const express = require("express");
const store = require("../store");

const router = express.Router();

// GET /api/timetable - all class entries belonging to the calling user
router.get("/", (req, res) => {
  const entries = store
    .getAll("timetable", req.userId)
    .sort((a, b) => (a.dayOfWeek + a.startTime).localeCompare(b.dayOfWeek + b.startTime));
  res.json(entries);
});

// POST /api/timetable - create a class entry
router.post("/", (req, res) => {
  const { moduleId, dayOfWeek, startTime, endTime, venue } = req.body;
  if (!moduleId || !dayOfWeek || !startTime || !endTime) {
    return res
      .status(400)
      .json({ error: "moduleId, dayOfWeek, startTime and endTime are required" });
  }

  const created = store.insert("timetable", req.userId, {
    moduleId,
    dayOfWeek,
    startTime,
    endTime,
    venue: venue || ""
  });
  res.status(201).json(created);
});

// PUT /api/timetable/:id - update a class entry
router.put("/:id", (req, res) => {
  const { moduleId, dayOfWeek, startTime, endTime, venue } = req.body;
  const updated = store.update("timetable", req.params.id, req.userId, {
    moduleId,
    dayOfWeek,
    startTime,
    endTime,
    venue: venue || ""
  });

  if (!updated) {
    return res.status(404).json({ error: "Timetable entry not found" });
  }
  res.json(updated);
});

// DELETE /api/timetable/:id
router.delete("/:id", (req, res) => {
  const removed = store.remove("timetable", req.params.id, req.userId);
  if (!removed) {
    return res.status(404).json({ error: "Timetable entry not found" });
  }
  res.status(204).send();
});

module.exports = router;

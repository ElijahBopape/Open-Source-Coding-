const express = require("express");
const store = require("../store");

const router = express.Router();

// GET /api/tasks - all tasks belonging to the calling user
router.get("/", (req, res) => {
  const tasks = store.getAll("tasks", req.userId).sort((a, b) => a.dueDate.localeCompare(b.dueDate));
  res.json(tasks);
});

// POST /api/tasks - create a task
router.post("/", (req, res) => {
  const { title, description, moduleId, dueDate, priority, completed } = req.body;
  if (!title || !title.trim()) {
    return res.status(400).json({ error: "title is required" });
  }
  if (!dueDate) {
    return res.status(400).json({ error: "dueDate is required" });
  }

  const created = store.insert("tasks", req.userId, {
    title,
    description: description || "",
    moduleId: moduleId ?? null,
    dueDate,
    priority: priority || "Medium",
    completed: !!completed
  });
  res.status(201).json(created);
});

// PUT /api/tasks/:id - update a task (also used to toggle "completed")
router.put("/:id", (req, res) => {
  const { title, description, moduleId, dueDate, priority, completed } = req.body;
  const updated = store.update("tasks", req.params.id, req.userId, {
    title,
    description: description || "",
    moduleId: moduleId ?? null,
    dueDate,
    priority: priority || "Medium",
    completed: !!completed
  });

  if (!updated) {
    return res.status(404).json({ error: "Task not found" });
  }
  res.json(updated);
});

// DELETE /api/tasks/:id
router.delete("/:id", (req, res) => {
  const removed = store.remove("tasks", req.params.id, req.userId);
  if (!removed) {
    return res.status(404).json({ error: "Task not found" });
  }
  res.status(204).send();
});

module.exports = router;

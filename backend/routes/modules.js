const express = require("express");
const store = require("../store");

const router = express.Router();

// GET /api/modules - all modules belonging to the calling user
router.get("/", (req, res) => {
  res.json(store.getAll("modules", req.userId));
});

// POST /api/modules - create a module
router.post("/", (req, res) => {
  const { name, code, lecturer, venue } = req.body;
  if (!name || !name.trim()) {
    return res.status(400).json({ error: "name is required" });
  }

  const created = store.insert("modules", req.userId, {
    name,
    code: code || "",
    lecturer: lecturer || "",
    venue: venue || ""
  });
  res.status(201).json(created);
});

// PUT /api/modules/:id - update a module owned by the calling user
router.put("/:id", (req, res) => {
  const { name, code, lecturer, venue } = req.body;
  const updated = store.update("modules", req.params.id, req.userId, {
    name,
    code: code || "",
    lecturer: lecturer || "",
    venue: venue || ""
  });

  if (!updated) {
    return res.status(404).json({ error: "Module not found" });
  }
  res.json(updated);
});

// DELETE /api/modules/:id
router.delete("/:id", (req, res) => {
  const removed = store.remove("modules", req.params.id, req.userId);
  if (!removed) {
    return res.status(404).json({ error: "Module not found" });
  }
  res.status(204).send();
});

module.exports = router;

// Builds a standard REST router (GET list, GET one, POST, PUT, DELETE) for one
// collection, so modules, tasks, timetable, exams and study sessions all share
// identical, tested behaviour instead of five copies of the same code.
//
// Each collection supplies:
//   validate(body)  -> an error message string, or null when the body is valid
//   normalise(body) -> the exact fields to store (whitelisting stops a client
//                      from writing arbitrary fields such as userId)
//   sort(a, b)      -> optional ordering for GET /
//   afterSave(req, row, isNew) -> optional hook, e.g. to send a push notification
//
// Pattern based on the Express Router guide: OpenJS Foundation (2026)
// https://expressjs.com/en/guide/routing.html
const express = require("express");
const store = require("../store");

function makeCrudRouter({ collection, label, validate, normalise, sort, afterSave }) {
  const router = express.Router();

  // Wraps async handlers so a rejected promise reaches the error middleware.
  const wrap = (fn) => (req, res, next) => Promise.resolve(fn(req, res, next)).catch(next);

  router.get(
    "/",
    wrap(async (req, res) => {
      const rows = await store.getAll(collection, req.userId);
      res.json(sort ? rows.sort(sort) : rows.sort((a, b) => a.id - b.id));
    })
  );

  router.get(
    "/:id",
    wrap(async (req, res) => {
      const row = await store.getOne(collection, req.params.id, req.userId);
      if (!row) return res.status(404).json({ error: `${label} not found` });
      res.json(row);
    })
  );

  router.post(
    "/",
    wrap(async (req, res) => {
      const error = validate(req.body || {});
      if (error) return res.status(400).json({ error });
      const created = await store.insert(collection, req.userId, normalise(req.body));
      console.log(`[${collection}] created #${created.id} for ${req.userId}`);
      if (afterSave) afterSave(req, created, true);
      res.status(201).json(created);
    })
  );

  router.put(
    "/:id",
    wrap(async (req, res) => {
      const error = validate(req.body || {});
      if (error) return res.status(400).json({ error });
      const updated = await store.update(collection, req.params.id, req.userId, normalise(req.body));
      if (!updated) return res.status(404).json({ error: `${label} not found` });
      console.log(`[${collection}] updated #${updated.id} for ${req.userId}`);
      if (afterSave) afterSave(req, updated, false);
      res.json(updated);
    })
  );

  router.delete(
    "/:id",
    wrap(async (req, res) => {
      const removed = await store.remove(collection, req.params.id, req.userId);
      if (!removed) return res.status(404).json({ error: `${label} not found` });
      console.log(`[${collection}] deleted #${req.params.id} for ${req.userId}`);
      res.status(204).send();
    })
  );

  return router;
}

// ---- shared validation helpers ------------------------------------------------
const ISO_DATE = /^\d{4}-\d{2}-\d{2}$/;
const TIME_24H = /^([01]\d|2[0-3]):[0-5]\d$/;
const PRIORITIES = ["High", "Medium", "Low"];
const DAYS = ["Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"];

const isBlank = (v) => v === undefined || v === null || String(v).trim() === "";
const optionalInt = (v) => (v === undefined || v === null || v === "" ? null : Number(v));
const trim = (v) => (v === undefined || v === null ? "" : String(v).trim());

module.exports = { makeCrudRouter, ISO_DATE, TIME_24H, PRIORITIES, DAYS, isBlank, optionalInt, trim };

// A small file-backed JSON data store. Chosen over a native database driver
// (e.g. better-sqlite3) so `npm install` never needs a C++ build toolchain -
// it works identically on the developer's machine, in CI and on Render's
// Linux build servers with zero native dependencies.
//
// Note: on a free Render web service the disk is not guaranteed to persist
// across redeploys (it does persist across requests while the service stays
// up, which is all a demo/marking session needs). For the Final PoE this can
// be swapped for a hosted database (e.g. Render PostgreSQL, Firebase
// Firestore) without changing the Android app's REST contract at all.
const fs = require("fs");
const path = require("path");

const DATA_FILE = path.join(__dirname, "data.json");
const COLLECTIONS = ["modules", "tasks", "timetable"];

function emptyData() {
  const nextId = {};
  const data = {};
  COLLECTIONS.forEach((name) => {
    data[name] = [];
    nextId[name] = 1;
  });
  data.nextId = nextId;
  return data;
}

function load() {
  if (!fs.existsSync(DATA_FILE)) {
    return emptyData();
  }
  try {
    return JSON.parse(fs.readFileSync(DATA_FILE, "utf8"));
  } catch (err) {
    console.error("data.json was unreadable, starting with an empty store:", err);
    return emptyData();
  }
}

function save(data) {
  fs.writeFileSync(DATA_FILE, JSON.stringify(data, null, 2));
}

function getAll(collection, userId) {
  const data = load();
  return data[collection].filter((row) => row.userId === userId);
}

function getOne(collection, id, userId) {
  const data = load();
  return data[collection].find((row) => row.id === Number(id) && row.userId === userId);
}

function insert(collection, userId, fields) {
  const data = load();
  const id = data.nextId[collection]++;
  const row = { id, userId, ...fields };
  data[collection].push(row);
  save(data);
  return row;
}

function update(collection, id, userId, fields) {
  const data = load();
  const index = data[collection].findIndex(
    (row) => row.id === Number(id) && row.userId === userId
  );
  if (index === -1) return null;

  data[collection][index] = { ...data[collection][index], ...fields };
  save(data);
  return data[collection][index];
}

function remove(collection, id, userId) {
  const data = load();
  const before = data[collection].length;
  data[collection] = data[collection].filter(
    (row) => !(row.id === Number(id) && row.userId === userId)
  );
  const removed = data[collection].length !== before;
  if (removed) save(data);
  return removed;
}

module.exports = { getAll, getOne, insert, update, remove };

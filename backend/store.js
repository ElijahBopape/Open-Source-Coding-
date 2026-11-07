// Data store used by every route. Two interchangeable drivers share one async
// interface (getAll / getOne / insert / update / remove / getDoc / setDoc):
//
//  * FirestoreDriver - Cloud Firestore, a hosted NoSQL document database. This is
//    what the deployed API on Render uses. Each record is a document in a
//    top-level collection (modules, tasks, timetable, exams, studySessions,
//    devices) tagged with the owner's Firebase UID, plus one "users/{uid}"
//    document holding the profile and settings.
//  * JsonFileDriver  - a local data.json file. Used automatically in unit tests,
//    CI and local development when no Firebase service account is configured.
//
// Records keep small integer ids (1, 2, 3 ...) in both drivers so the Android
// app's REST contract from Part 2 did not have to change. In Firestore the next
// id is allocated inside a transaction on a "counters/{collection}" document so
// two simultaneous requests can never receive the same id.
//
// References:
//   Google (2026b) Add data to Cloud Firestore. https://firebase.google.com/docs/firestore/manage-data/add-data
//   Google (2026c) Transactions and batched writes. https://firebase.google.com/docs/firestore/manage-data/transactions
const fs = require("fs");
const path = require("path");
const firebase = require("./firebase");

const COLLECTIONS = ["modules", "tasks", "timetable", "exams", "studySessions", "devices"];

// ---------------------------------------------------------------------------
// Local JSON driver
// ---------------------------------------------------------------------------
class JsonFileDriver {
  constructor(file) {
    this.file = file;
    this.name = "json-file";
  }

  emptyData() {
    const data = { nextId: {}, docs: {} };
    COLLECTIONS.forEach((name) => {
      data[name] = [];
      data.nextId[name] = 1;
    });
    return data;
  }

  load() {
    if (!fs.existsSync(this.file)) return this.emptyData();
    try {
      const data = JSON.parse(fs.readFileSync(this.file, "utf8"));
      // Upgrade a Part 2 data.json that predates the new collections.
      const fresh = this.emptyData();
      COLLECTIONS.forEach((name) => {
        data[name] = data[name] || fresh[name];
        data.nextId = data.nextId || {};
        data.nextId[name] = data.nextId[name] || 1;
      });
      data.docs = data.docs || {};
      return data;
    } catch (err) {
      console.error("[store] data.json was unreadable, starting with an empty store:", err.message);
      return this.emptyData();
    }
  }

  save(data) {
    fs.writeFileSync(this.file, JSON.stringify(data, null, 2));
  }

  async getAll(collection, userId) {
    return this.load()[collection].filter((row) => row.userId === userId);
  }

  async getOne(collection, id, userId) {
    return this.load()[collection].find((row) => row.id === Number(id) && row.userId === userId) || null;
  }

  async insert(collection, userId, fields) {
    const data = this.load();
    const id = data.nextId[collection]++;
    const now = new Date().toISOString();
    const row = { id, userId, ...fields, createdAt: now, updatedAt: now };
    data[collection].push(row);
    this.save(data);
    return row;
  }

  async update(collection, id, userId, fields) {
    const data = this.load();
    const index = data[collection].findIndex((row) => row.id === Number(id) && row.userId === userId);
    if (index === -1) return null;
    data[collection][index] = {
      ...data[collection][index],
      ...fields,
      id: Number(id),
      userId,
      updatedAt: new Date().toISOString()
    };
    this.save(data);
    return data[collection][index];
  }

  async remove(collection, id, userId) {
    const data = this.load();
    const before = data[collection].length;
    data[collection] = data[collection].filter((row) => !(row.id === Number(id) && row.userId === userId));
    const removed = data[collection].length !== before;
    if (removed) this.save(data);
    return removed;
  }

  async getDoc(collection, docId) {
    const data = this.load();
    return (data.docs[collection] && data.docs[collection][docId]) || null;
  }

  async setDoc(collection, docId, fields) {
    const data = this.load();
    data.docs[collection] = data.docs[collection] || {};
    data.docs[collection][docId] = { ...(data.docs[collection][docId] || {}), ...fields };
    this.save(data);
    return data.docs[collection][docId];
  }

  async findByField(collection, field, value) {
    return this.load()[collection].filter((row) => row[field] === value);
  }

  async removeWhere(collection, field, value) {
    const data = this.load();
    data[collection] = data[collection].filter((row) => row[field] !== value);
    this.save(data);
  }
}

// ---------------------------------------------------------------------------
// Cloud Firestore driver
// ---------------------------------------------------------------------------
class FirestoreDriver {
  constructor(db) {
    this.db = db;
    this.name = "firestore";
  }

  docRef(collection, id) {
    return this.db.collection(collection).doc(String(id));
  }

  async getAll(collection, userId) {
    const snapshot = await this.db.collection(collection).where("userId", "==", userId).get();
    return snapshot.docs.map((doc) => doc.data());
  }

  async getOne(collection, id, userId) {
    const doc = await this.docRef(collection, id).get();
    if (!doc.exists || doc.data().userId !== userId) return null;
    return doc.data();
  }

  async insert(collection, userId, fields) {
    const counterRef = this.db.collection("counters").doc(collection);
    const now = new Date().toISOString();

    return this.db.runTransaction(async (tx) => {
      const counter = await tx.get(counterRef);
      const id = counter.exists ? counter.data().next : 1;
      const row = { id, userId, ...fields, createdAt: now, updatedAt: now };
      tx.set(counterRef, { next: id + 1 });
      tx.set(this.docRef(collection, id), row);
      return row;
    });
  }

  async update(collection, id, userId, fields) {
    const ref = this.docRef(collection, id);
    const existing = await ref.get();
    if (!existing.exists || existing.data().userId !== userId) return null;
    const row = { ...existing.data(), ...fields, id: Number(id), userId, updatedAt: new Date().toISOString() };
    await ref.set(row);
    return row;
  }

  async remove(collection, id, userId) {
    const ref = this.docRef(collection, id);
    const existing = await ref.get();
    if (!existing.exists || existing.data().userId !== userId) return false;
    await ref.delete();
    return true;
  }

  async getDoc(collection, docId) {
    const doc = await this.docRef(collection, docId).get();
    return doc.exists ? doc.data() : null;
  }

  async setDoc(collection, docId, fields) {
    const ref = this.docRef(collection, docId);
    await ref.set(fields, { merge: true });
    return (await ref.get()).data();
  }

  async findByField(collection, field, value) {
    const snapshot = await this.db.collection(collection).where(field, "==", value).get();
    return snapshot.docs.map((doc) => doc.data());
  }

  async removeWhere(collection, field, value) {
    const snapshot = await this.db.collection(collection).where(field, "==", value).get();
    const batch = this.db.batch();
    snapshot.docs.forEach((doc) => batch.delete(doc.ref));
    await batch.commit();
  }
}

// ---------------------------------------------------------------------------
// Driver selection
// ---------------------------------------------------------------------------
let driver = null;

function getDriver() {
  if (driver) return driver;
  if (process.env.STORE !== "json" && firebase.isAvailable()) {
    driver = new FirestoreDriver(firebase.admin.firestore());
  } else {
    const file = process.env.DATA_FILE || path.join(__dirname, "data.json");
    driver = new JsonFileDriver(file);
  }
  console.log(`[store] Using the ${driver.name} driver`);
  return driver;
}

module.exports = {
  COLLECTIONS,
  driverName: () => getDriver().name,
  getAll: (...args) => getDriver().getAll(...args),
  getOne: (...args) => getDriver().getOne(...args),
  insert: (...args) => getDriver().insert(...args),
  update: (...args) => getDriver().update(...args),
  remove: (...args) => getDriver().remove(...args),
  getDoc: (...args) => getDriver().getDoc(...args),
  setDoc: (...args) => getDriver().setDoc(...args),
  findByField: (...args) => getDriver().findByField(...args),
  removeWhere: (...args) => getDriver().removeWhere(...args)
};

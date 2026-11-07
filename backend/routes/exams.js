// /api/exams - examination management (Part 1 design, section 3.6) with an
// optional mark once written, used for academic progress (section 3.7).
const { makeCrudRouter, ISO_DATE, TIME_24H, isBlank, optionalInt, trim } = require("./crud");
const notify = require("../notify");

module.exports = makeCrudRouter({
  collection: "exams",
  label: "Exam",
  validate: (b) => {
    if (isBlank(b.title)) return "title is required";
    if (!ISO_DATE.test(b.date || "")) return "date must be yyyy-MM-dd";
    if (!isBlank(b.time) && !TIME_24H.test(b.time)) return "time must be HH:mm";
    if (!isBlank(b.durationMinutes) && !(Number(b.durationMinutes) > 0)) return "durationMinutes must be positive";
    if (!isBlank(b.mark) && !(Number(b.mark) >= 0 && Number(b.mark) <= 100)) return "mark must be 0-100";
    return null;
  },
  normalise: (b) => ({
    title: trim(b.title),
    moduleId: optionalInt(b.moduleId),
    date: b.date,
    time: trim(b.time),
    venue: trim(b.venue),
    durationMinutes: optionalInt(b.durationMinutes) || 120,
    mark: isBlank(b.mark) ? null : Number(b.mark)
  }),
  sort: (a, b) => (a.date + a.time).localeCompare(b.date + b.time),
  afterSave: (req, exam, isNew) => {
    if (isNew) {
      notify.sendToUser(req.userId, "Exam scheduled", `${exam.title} on ${exam.date}${exam.time ? " at " + exam.time : ""}`, {
        type: "exam",
        id: exam.id
      });
    }
  }
});

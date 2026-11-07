// /api/study-sessions - the Study Planner (Part 1 design, sections 3.8 and 3.12):
// large pieces of work are broken into smaller, scheduled study sessions that
// can be linked to a task or an exam.
const { makeCrudRouter, ISO_DATE, TIME_24H, isBlank, optionalInt, trim } = require("./crud");

module.exports = makeCrudRouter({
  collection: "studySessions",
  label: "Study session",
  validate: (b) => {
    if (isBlank(b.title)) return "title is required";
    if (!ISO_DATE.test(b.date || "")) return "date must be yyyy-MM-dd";
    if (!isBlank(b.startTime) && !TIME_24H.test(b.startTime)) return "startTime must be HH:mm";
    if (!(Number(b.durationMinutes) > 0)) return "durationMinutes must be positive";
    return null;
  },
  normalise: (b) => ({
    title: trim(b.title),
    date: b.date,
    startTime: trim(b.startTime),
    durationMinutes: Number(b.durationMinutes),
    moduleId: optionalInt(b.moduleId),
    taskId: optionalInt(b.taskId),
    examId: optionalInt(b.examId),
    completed: !!b.completed
  }),
  sort: (a, b) => (a.date + a.startTime).localeCompare(b.date + b.startTime)
});

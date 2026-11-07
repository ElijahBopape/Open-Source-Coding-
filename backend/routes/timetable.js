// /api/timetable - recurring weekly classes (Part 1 design, section 3.4).
const { makeCrudRouter, TIME_24H, DAYS, isBlank, optionalInt, trim } = require("./crud");

module.exports = makeCrudRouter({
  collection: "timetable",
  label: "Timetable entry",
  validate: (b) => {
    if (isBlank(b.moduleId)) return "moduleId is required";
    if (!DAYS.includes(b.dayOfWeek)) return "dayOfWeek must be Monday-Sunday";
    if (!TIME_24H.test(b.startTime || "") || !TIME_24H.test(b.endTime || "")) return "times must be HH:mm";
    if (b.endTime <= b.startTime) return "endTime must be after startTime";
    return null;
  },
  normalise: (b) => ({
    moduleId: optionalInt(b.moduleId),
    dayOfWeek: b.dayOfWeek,
    startTime: b.startTime,
    endTime: b.endTime,
    venue: trim(b.venue)
  }),
  sort: (a, b) => a.startTime.localeCompare(b.startTime)
});

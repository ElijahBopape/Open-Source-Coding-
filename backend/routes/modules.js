// /api/modules - the user's academic modules (Part 1 design, section 3.3).
const { makeCrudRouter, isBlank, trim } = require("./crud");

module.exports = makeCrudRouter({
  collection: "modules",
  label: "Module",
  validate: (b) => (isBlank(b.name) ? "name is required" : null),
  normalise: (b) => ({
    name: trim(b.name),
    code: trim(b.code).toUpperCase(),
    lecturer: trim(b.lecturer),
    venue: trim(b.venue)
  }),
  sort: (a, b) => a.name.localeCompare(b.name)
});

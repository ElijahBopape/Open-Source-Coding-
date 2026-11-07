// /api/tasks - assignments and academic tasks (Part 1 design, section 3.5).
// Saving a new, incomplete task sends a real-time push to the user's devices.
const { makeCrudRouter, ISO_DATE, PRIORITIES, isBlank, optionalInt, trim } = require("./crud");
const notify = require("../notify");

module.exports = makeCrudRouter({
  collection: "tasks",
  label: "Task",
  validate: (b) => {
    if (isBlank(b.title)) return "title is required";
    if (!ISO_DATE.test(b.dueDate || "")) return "dueDate must be yyyy-MM-dd";
    if (b.priority && !PRIORITIES.includes(b.priority)) return "priority must be High, Medium or Low";
    return null;
  },
  normalise: (b) => ({
    title: trim(b.title),
    description: trim(b.description),
    moduleId: optionalInt(b.moduleId),
    dueDate: b.dueDate,
    priority: b.priority || "Medium",
    completed: !!b.completed
  }),
  sort: (a, b) => a.dueDate.localeCompare(b.dueDate),
  afterSave: (req, task, isNew) => {
    if (isNew && !task.completed) {
      notify.sendToUser(req.userId, "New task added", `${task.title} - due ${task.dueDate} (${task.priority})`, {
        type: "task",
        id: task.id
      });
    }
  }
});

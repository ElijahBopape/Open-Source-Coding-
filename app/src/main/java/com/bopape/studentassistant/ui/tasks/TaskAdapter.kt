package com.bopape.studentassistant.ui.tasks

import android.annotation.SuppressLint
import android.content.res.ColorStateList
import android.graphics.Paint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bopape.studentassistant.R
import com.bopape.studentassistant.data.model.Module
import com.bopape.studentassistant.data.model.Priority
import com.bopape.studentassistant.data.model.Task
import com.bopape.studentassistant.databinding.ItemTaskBinding
import com.bopape.studentassistant.util.DateUtils
import com.bopape.studentassistant.util.PriorityUtils
import com.bopape.studentassistant.util.displayDate
import com.bopape.studentassistant.util.priorityLabel

/**
 * Task rows for the Tasks screen and the dashboard. Shows the priority badge, a
 * cloud icon while the task is waiting to be synced (offline mode), a red due
 * date when overdue, and an "Exam soon" note when the exam-proximity boost of the
 * Academic Priority System has raised the task's priority.
 */
class TaskAdapter(
    private val onClick: (Task) -> Unit,
    private val onDelete: (Task) -> Unit,
    private val onToggleComplete: (Task, Boolean) -> Unit
) : RecyclerView.Adapter<TaskAdapter.TaskViewHolder>() {

    private val tasks = mutableListOf<Task>()
    private var modulesById: Map<Int, Module> = emptyMap()
    private var examModules: Set<Int> = emptySet()

    @SuppressLint("NotifyDataSetChanged")
    fun submitList(newTasks: List<Task>) {
        tasks.clear()
        tasks.addAll(newTasks)
        notifyDataSetChanged()
    }

    @SuppressLint("NotifyDataSetChanged")
    fun setModules(modules: List<Module>) {
        modulesById = modules.associateBy { it.id }
        notifyDataSetChanged()
    }

    @SuppressLint("NotifyDataSetChanged")
    fun setExamModules(moduleIds: Set<Int>) {
        examModules = moduleIds
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val binding = ItemTaskBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return TaskViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        holder.bind(tasks[position])
    }

    override fun getItemCount(): Int = tasks.size

    inner class TaskViewHolder(private val binding: ItemTaskBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(task: Task) {
            val context = binding.root.context
            binding.textTaskTitle.text = task.title
            binding.textTaskTitle.paintFlags = if (task.completed) {
                binding.textTaskTitle.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
            } else {
                binding.textTaskTitle.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
            }

            val moduleName = task.moduleId?.let { modulesById[it]?.code?.ifBlank { modulesById[it]?.name } }
            val dueLabel = context.getString(R.string.task_due, context.displayDate(task.dueDate))
            binding.textTaskSubtitle.text = listOfNotNull(moduleName, dueLabel).joinToString(" · ")

            val overdue = !task.completed && (DateUtils.daysUntil(task.dueDate) ?: 0) < 0
            binding.textTaskSubtitle.setTextColor(
                ContextCompat.getColor(context, if (overdue) R.color.priority_high else R.color.on_surface_muted)
            )

            binding.textPriorityBadge.text = context.priorityLabel(task.priority)
            val colorRes = when (task.priority) {
                Priority.HIGH -> R.color.priority_high
                Priority.LOW -> R.color.priority_low
                else -> R.color.priority_medium
            }
            binding.textPriorityBadge.backgroundTintList =
                ColorStateList.valueOf(ContextCompat.getColor(context, colorRes))

            binding.textExamBoost.visibility =
                if (PriorityUtils.isExamBoosted(task, examModules)) View.VISIBLE else View.GONE
            binding.iconPendingSync.visibility = if (task.pendingSync) View.VISIBLE else View.GONE

            // Avoid re-triggering the listener while RecyclerView recycles/binds this row.
            binding.checkboxCompleted.setOnCheckedChangeListener(null)
            binding.checkboxCompleted.isChecked = task.completed
            binding.checkboxCompleted.setOnCheckedChangeListener { _, isChecked ->
                onToggleComplete(task, isChecked)
            }

            binding.root.setOnClickListener { onClick(task) }
            binding.buttonDeleteTask.setOnClickListener { onDelete(task) }
        }
    }
}

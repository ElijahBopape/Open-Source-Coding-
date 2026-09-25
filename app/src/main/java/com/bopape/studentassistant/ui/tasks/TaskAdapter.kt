package com.bopape.studentassistant.ui.tasks

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bopape.studentassistant.R
import com.bopape.studentassistant.data.model.Module
import com.bopape.studentassistant.data.model.Priority
import com.bopape.studentassistant.data.model.Task
import com.bopape.studentassistant.databinding.ItemTaskBinding
import com.bopape.studentassistant.util.DateUtils

class TaskAdapter(
    private val onClick: (Task) -> Unit,
    private val onDelete: (Task) -> Unit,
    private val onToggleComplete: (Task, Boolean) -> Unit
) : RecyclerView.Adapter<TaskAdapter.TaskViewHolder>() {

    private val tasks = mutableListOf<Task>()
    private var modulesById: Map<Int, Module> = emptyMap()

    fun submitList(newTasks: List<Task>) {
        tasks.clear()
        tasks.addAll(newTasks)
        notifyDataSetChanged()
    }

    fun setModules(modules: List<Module>) {
        modulesById = modules.associateBy { it.id }
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
            binding.textTaskTitle.text = task.title

            val moduleName = task.moduleId?.let { modulesById[it]?.name }
            val dueLabel = "Due ${DateUtils.formatIsoDateForDisplay(task.dueDate)}"
            binding.textTaskSubtitle.text = listOfNotNull(moduleName, dueLabel).joinToString(" · ")

            binding.textPriorityBadge.text = task.priority
            val colorRes = when (task.priority) {
                Priority.HIGH -> R.color.priority_high
                Priority.LOW -> R.color.priority_low
                else -> R.color.priority_medium
            }
            binding.textPriorityBadge.backgroundTintList = ColorStateList.valueOf(
                ContextCompat.getColor(binding.root.context, colorRes)
            )

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

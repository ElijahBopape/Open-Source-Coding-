package com.bopape.studentassistant.ui.tasks

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.bopape.studentassistant.R
import com.bopape.studentassistant.data.model.Module
import com.bopape.studentassistant.data.model.Priority
import com.bopape.studentassistant.data.model.Task
import com.bopape.studentassistant.data.repository.ModuleRepository
import com.bopape.studentassistant.data.repository.TaskRepository
import com.bopape.studentassistant.databinding.FragmentTasksBinding
import com.bopape.studentassistant.util.ApiResult
import com.bopape.studentassistant.util.DateUtils
import com.bopape.studentassistant.util.PriorityUtils
import kotlinx.coroutines.launch

/** Lists tasks, applies the priority/today/done filters and lets the user manage them. */
class TasksFragment : Fragment() {

    private var _binding: FragmentTasksBinding? = null
    private val binding get() = _binding!!

    private val taskRepository = TaskRepository()
    private val moduleRepository = ModuleRepository()
    private lateinit var adapter: TaskAdapter

    private var allTasks: List<Task> = emptyList()
    private var currentFilter = Filter.ALL
    private val tag = "TasksFragment"

    private enum class Filter { ALL, HIGH, TODAY, DONE }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTasksBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = TaskAdapter(
            onClick = { openEditor(it) },
            onDelete = { confirmDelete(it) },
            onToggleComplete = { task, isChecked -> toggleComplete(task, isChecked) }
        )
        binding.recyclerTasks.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerTasks.adapter = adapter

        binding.fabAddTask.setOnClickListener {
            startActivity(Intent(requireContext(), AddEditTaskActivity::class.java))
        }

        binding.chipGroupFilter.setOnCheckedStateChangeListener { _, checkedIds ->
            currentFilter = when (checkedIds.firstOrNull()) {
                R.id.chipHigh -> Filter.HIGH
                R.id.chipToday -> Filter.TODAY
                R.id.chipDone -> Filter.DONE
                else -> Filter.ALL
            }
            applyFilter()
        }
    }

    override fun onResume() {
        super.onResume()
        loadModules()
        loadTasks()
    }

    private fun loadModules() {
        lifecycleScope.launch {
            when (val result = moduleRepository.getModules()) {
                is ApiResult.Success -> adapter.setModules(result.data)
                is ApiResult.Failure -> Log.w(tag, "Could not load modules for task list: ${result.message}")
            }
        }
    }

    private fun loadTasks() {
        lifecycleScope.launch {
            when (val result = taskRepository.getTasks()) {
                is ApiResult.Success -> {
                    Log.d(tag, "Loaded ${result.data.size} tasks")
                    allTasks = result.data
                    applyFilter()
                }
                is ApiResult.Failure -> {
                    Log.e(tag, "Failed to load tasks: ${result.message}")
                    Toast.makeText(requireContext(), result.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun applyFilter() {
        val filtered = when (currentFilter) {
            Filter.ALL -> allTasks.filter { !it.completed }
            Filter.HIGH -> allTasks.filter { !it.completed && it.priority == Priority.HIGH }
            Filter.TODAY -> allTasks.filter { !it.completed && it.dueDate == DateUtils.todayIso() }
            Filter.DONE -> allTasks.filter { it.completed }
        }
        val sorted = PriorityUtils.sortByPriority(filtered)
        adapter.submitList(sorted)
        binding.textNoTasks.visibility = if (sorted.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun openEditor(task: Task) {
        val intent = Intent(requireContext(), AddEditTaskActivity::class.java).apply {
            putExtra(AddEditTaskActivity.EXTRA_TASK_ID, task.id)
            putExtra(AddEditTaskActivity.EXTRA_TASK_TITLE, task.title)
            putExtra(AddEditTaskActivity.EXTRA_TASK_DESCRIPTION, task.description)
            putExtra(AddEditTaskActivity.EXTRA_TASK_MODULE_ID, task.moduleId ?: -1)
            putExtra(AddEditTaskActivity.EXTRA_TASK_DUE_DATE, task.dueDate)
            putExtra(AddEditTaskActivity.EXTRA_TASK_PRIORITY, task.priority)
            putExtra(AddEditTaskActivity.EXTRA_TASK_COMPLETED, task.completed)
        }
        startActivity(intent)
    }

    private fun toggleComplete(task: Task, isChecked: Boolean) {
        lifecycleScope.launch {
            when (val result = taskRepository.updateTask(task.copy(completed = isChecked))) {
                is ApiResult.Success -> loadTasks()
                is ApiResult.Failure -> Toast.makeText(requireContext(), result.message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun confirmDelete(task: Task) {
        AlertDialog.Builder(requireContext())
            .setTitle(task.title)
            .setMessage(R.string.delete)
            .setPositiveButton(R.string.delete) { _, _ -> deleteTask(task) }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun deleteTask(task: Task) {
        lifecycleScope.launch {
            when (val result = taskRepository.deleteTask(task.id)) {
                is ApiResult.Success -> loadTasks()
                is ApiResult.Failure -> Toast.makeText(requireContext(), result.message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

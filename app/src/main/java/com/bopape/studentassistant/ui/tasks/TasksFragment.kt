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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.bopape.studentassistant.R
import com.bopape.studentassistant.data.model.Exam
import com.bopape.studentassistant.data.model.Priority
import com.bopape.studentassistant.data.model.Task
import com.bopape.studentassistant.data.repository.ExamRepository
import com.bopape.studentassistant.data.repository.ModuleRepository
import com.bopape.studentassistant.data.repository.TaskRepository
import com.bopape.studentassistant.data.sync.SyncBus
import com.bopape.studentassistant.data.sync.SyncScheduler
import com.bopape.studentassistant.databinding.FragmentTasksBinding
import com.bopape.studentassistant.util.ApiResult
import com.bopape.studentassistant.util.DateUtils
import com.bopape.studentassistant.util.PriorityUtils
import kotlinx.coroutines.launch

/**
 * Lists tasks, applies the All/High/Today/Done filters and lets the user manage
 * them. This screen is fully offline-capable: tasks are read from and written to
 * Room, and changes are synced to the API in the background.
 */
class TasksFragment : Fragment() {

    private var _binding: FragmentTasksBinding? = null
    private val binding get() = _binding!!

    private val taskRepository = TaskRepository()
    private val moduleRepository = ModuleRepository()
    private val examRepository = ExamRepository()
    private lateinit var adapter: TaskAdapter

    private var allTasks: List<Task> = emptyList()
    private var exams: List<Exam> = emptyList()
    private var currentFilter = Filter.ALL
    private val tag = "TasksFragment"

    private enum class Filter { ALL, HIGH, TODAY, DONE }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentTasksBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = TaskAdapter(
            onClick = { openEditor(this, it) },
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

        binding.swipeRefresh.setOnRefreshListener { syncNow() }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                SyncBus.events.collect { loadTasks() }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        loadSupportingData()
        loadTasks()
        SyncScheduler.requestSync(requireContext())
    }

    private fun loadSupportingData() {
        viewLifecycleOwner.lifecycleScope.launch {
            when (val result = moduleRepository.getModules()) {
                is ApiResult.Success -> adapter.setModules(result.data)
                is ApiResult.Failure -> Log.w(tag, "Could not load modules for task list: ${result.message}")
            }
            exams = (examRepository.getExams() as? ApiResult.Success)?.data.orEmpty()
            adapter.setExamModules(PriorityUtils.modulesWithExamSoon(exams))
            applyFilter()
        }
    }

    private fun loadTasks() {
        viewLifecycleOwner.lifecycleScope.launch {
            val result = taskRepository.getTasks() as ApiResult.Success
            Log.d(tag, "Loaded ${result.data.size} tasks from Room")
            allTasks = result.data
            applyFilter()
        }
    }

    /** Pull-to-refresh: upload pending changes and download the latest from the API now. */
    private fun syncNow() {
        viewLifecycleOwner.lifecycleScope.launch {
            val outcome = taskRepository.syncNow()
            _binding?.swipeRefresh?.isRefreshing = false
            if (!outcome.success) {
                Toast.makeText(requireContext(), R.string.sync_offline_message, Toast.LENGTH_SHORT).show()
            }
            loadTasks()
        }
    }

    private fun applyFilter() {
        if (_binding == null) return
        val today = DateUtils.todayIso()
        val filtered = when (currentFilter) {
            Filter.ALL -> allTasks.filter { !it.completed }
            Filter.HIGH -> allTasks.filter { !it.completed && it.priority == Priority.HIGH }
            Filter.TODAY -> allTasks.filter { !it.completed && it.dueDate <= today }
            Filter.DONE -> allTasks.filter { it.completed }
        }
        val sorted = PriorityUtils.sortByPriority(filtered, exams)
        adapter.submitList(sorted)
        binding.textNoTasks.visibility = if (sorted.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun toggleComplete(task: Task, isChecked: Boolean) {
        viewLifecycleOwner.lifecycleScope.launch {
            taskRepository.updateTask(task.copy(completed = isChecked))
            loadTasks()
        }
    }

    private fun confirmDelete(task: Task) {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.delete_task_confirm_title)
            .setMessage(getString(R.string.delete_task_confirm_message, task.title))
            .setPositiveButton(R.string.delete) { _, _ ->
                viewLifecycleOwner.lifecycleScope.launch {
                    taskRepository.deleteTask(task.id)
                    loadTasks()
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        /** Opens the task editor for [task]; shared with the dashboard. */
        fun openEditor(fragment: Fragment, task: Task) {
            val intent = Intent(fragment.requireContext(), AddEditTaskActivity::class.java).apply {
                putExtra(AddEditTaskActivity.EXTRA_TASK_ID, task.id)
                putExtra(AddEditTaskActivity.EXTRA_TASK_TITLE, task.title)
                putExtra(AddEditTaskActivity.EXTRA_TASK_DESCRIPTION, task.description)
                putExtra(AddEditTaskActivity.EXTRA_TASK_MODULE_ID, task.moduleId ?: -1)
                putExtra(AddEditTaskActivity.EXTRA_TASK_DUE_DATE, task.dueDate)
                putExtra(AddEditTaskActivity.EXTRA_TASK_PRIORITY, task.priority)
                putExtra(AddEditTaskActivity.EXTRA_TASK_COMPLETED, task.completed)
            }
            fragment.startActivity(intent)
        }
    }
}

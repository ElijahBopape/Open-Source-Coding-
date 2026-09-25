package com.bopape.studentassistant.ui.dashboard

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.bopape.studentassistant.R
import com.bopape.studentassistant.data.model.Module
import com.bopape.studentassistant.data.model.TimetableEntry
import com.bopape.studentassistant.data.repository.ModuleRepository
import com.bopape.studentassistant.data.repository.TaskRepository
import com.bopape.studentassistant.data.repository.TimetableRepository
import com.bopape.studentassistant.databinding.FragmentDashboardBinding
import com.bopape.studentassistant.ui.tasks.AddEditTaskActivity
import com.bopape.studentassistant.ui.tasks.TaskAdapter
import com.bopape.studentassistant.ui.timetable.TimetableFragment
import com.bopape.studentassistant.util.ApiResult
import com.bopape.studentassistant.util.PriorityUtils
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

/** The landing screen: next class, greeting and the highest-priority pending tasks. */
class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!

    private val taskRepository = TaskRepository()
    private val moduleRepository = ModuleRepository()
    private val timetableRepository = TimetableRepository()
    private lateinit var adapter: TaskAdapter
    private val tag = "DashboardFragment"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val name = FirebaseAuth.getInstance().currentUser?.displayName
        binding.textGreeting.text = if (name.isNullOrBlank()) {
            getString(R.string.dashboard_greeting_prefix)
        } else {
            "${getString(R.string.dashboard_greeting_prefix)} $name"
        }

        adapter = TaskAdapter(
            onClick = { startActivity(Intent(requireContext(), AddEditTaskActivity::class.java)) },
            onDelete = { task ->
                lifecycleScope.launch {
                    taskRepository.deleteTask(task.id)
                    loadDashboard()
                }
            },
            onToggleComplete = { task, isChecked ->
                lifecycleScope.launch {
                    taskRepository.updateTask(task.copy(completed = isChecked))
                    loadDashboard()
                }
            }
        )
        binding.recyclerPriorityTasks.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerPriorityTasks.adapter = adapter

        binding.swipeRefresh.setOnRefreshListener { loadDashboard() }
    }

    override fun onResume() {
        super.onResume()
        loadDashboard()
    }

    private fun loadDashboard() {
        lifecycleScope.launch {
            val modulesResult = moduleRepository.getModules()
            val modules = (modulesResult as? ApiResult.Success)?.data ?: emptyList()
            adapter.setModules(modules)

            loadNextClass(modules)
            loadPriorityTasks()

            binding.swipeRefresh.isRefreshing = false
        }
    }

    private suspend fun loadNextClass(modules: List<Module>) {
        when (val result = timetableRepository.getTimetable()) {
            is ApiResult.Success -> {
                val next = findNextClass(result.data)
                if (next == null) {
                    binding.textNextClass.text = getString(R.string.dashboard_no_class_today)
                    binding.textNextClassDetail.text = ""
                } else {
                    val moduleName = modules.find { it.id == next.moduleId }?.name ?: "Class"
                    binding.textNextClass.text = moduleName
                    binding.textNextClassDetail.text = "${next.startTime} – ${next.endTime} · ${next.venue}"
                }
            }
            is ApiResult.Failure -> Log.w(tag, "Could not load timetable for dashboard: ${result.message}")
        }
    }

    private fun findNextClass(entries: List<TimetableEntry>): TimetableEntry? {
        val today = TimetableFragment.currentDayOfWeek()
        val nowTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(java.util.Date())
        return entries
            .filter { it.dayOfWeek == today && it.startTime >= nowTime }
            .minByOrNull { it.startTime }
    }

    private suspend fun loadPriorityTasks() {
        when (val result = taskRepository.getTasks()) {
            is ApiResult.Success -> {
                val topTasks = PriorityUtils.topPriorityTasks(result.data)
                adapter.submitList(topTasks)
                binding.textNoTasks.visibility = if (topTasks.isEmpty()) View.VISIBLE else View.GONE
            }
            is ApiResult.Failure -> {
                Log.e(tag, "Failed to load tasks for dashboard: ${result.message}")
                Toast.makeText(requireContext(), result.message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

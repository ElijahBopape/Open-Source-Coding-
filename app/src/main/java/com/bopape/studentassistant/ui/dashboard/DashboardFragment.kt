package com.bopape.studentassistant.ui.dashboard

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.bopape.studentassistant.R
import com.bopape.studentassistant.data.model.Exam
import com.bopape.studentassistant.data.model.Module
import com.bopape.studentassistant.data.model.StudySession
import com.bopape.studentassistant.data.model.TimetableEntry
import com.bopape.studentassistant.data.repository.ExamRepository
import com.bopape.studentassistant.data.repository.ModuleRepository
import com.bopape.studentassistant.data.repository.StudySessionRepository
import com.bopape.studentassistant.data.repository.TaskRepository
import com.bopape.studentassistant.data.repository.TimetableRepository
import com.bopape.studentassistant.data.sync.SyncBus
import com.bopape.studentassistant.data.sync.SyncScheduler
import com.bopape.studentassistant.databinding.FragmentDashboardBinding
import com.bopape.studentassistant.ui.main.FragmentHostActivity
import com.bopape.studentassistant.ui.main.MainActivity
import com.bopape.studentassistant.ui.tasks.TaskAdapter
import com.bopape.studentassistant.ui.tasks.TasksFragment
import com.bopape.studentassistant.util.ApiResult
import com.bopape.studentassistant.util.DateUtils
import com.bopape.studentassistant.util.PriorityUtils
import com.bopape.studentassistant.util.StudyPlanner
import com.bopape.studentassistant.util.displayDate
import com.bopape.studentassistant.util.relativeDays
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

/**
 * The landing screen. It connects all the features in one place, as the Part 1
 * design intended: the next class (timetable), the nearest exam with a countdown,
 * academic progress (average exam mark), today's study sessions, and the
 * highest-priority pending tasks from the Academic Priority System.
 */
class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!

    private val taskRepository = TaskRepository()
    private val moduleRepository = ModuleRepository()
    private val timetableRepository = TimetableRepository()
    private val examRepository = ExamRepository()
    private val studyRepository = StudySessionRepository()
    private lateinit var adapter: TaskAdapter
    private val tag = "DashboardFragment"

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val name = FirebaseAuth.getInstance().currentUser?.displayName?.substringBefore(' ')
        binding.textGreeting.text = if (name.isNullOrBlank()) {
            getString(R.string.dashboard_greeting_no_name)
        } else {
            getString(R.string.dashboard_greeting, name)
        }

        adapter = TaskAdapter(
            onClick = { TasksFragment.openEditor(this, it) },
            onDelete = { task ->
                viewLifecycleOwner.lifecycleScope.launch {
                    taskRepository.deleteTask(task.id)
                    loadDashboard()
                }
            },
            onToggleComplete = { task, isChecked ->
                viewLifecycleOwner.lifecycleScope.launch {
                    taskRepository.updateTask(task.copy(completed = isChecked))
                    loadDashboard()
                }
            }
        )
        binding.recyclerPriorityTasks.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerPriorityTasks.adapter = adapter

        binding.cardModules.setOnClickListener {
            startActivity(FragmentHostActivity.intent(requireContext(), FragmentHostActivity.SCREEN_MODULES))
        }
        binding.cardStudyPlanner.setOnClickListener {
            startActivity(FragmentHostActivity.intent(requireContext(), FragmentHostActivity.SCREEN_STUDY))
        }
        binding.cardNextExam.setOnClickListener { (activity as? MainActivity)?.selectTab(R.id.nav_exams) }
        binding.textSeeAllTasks.setOnClickListener { (activity as? MainActivity)?.selectTab(R.id.nav_tasks) }

        binding.swipeRefresh.setOnRefreshListener {
            SyncScheduler.requestSync(requireContext())
            loadDashboard()
        }

        // Reload when a background sync brings in new data.
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                SyncBus.events.collect { loadDashboard() }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        loadDashboard()
    }

    private fun loadDashboard() {
        viewLifecycleOwner.lifecycleScope.launch {
            // Load all sources in parallel so the dashboard appears quickly.
            coroutineScope {
                val modulesJob = async { (moduleRepository.getModules() as? ApiResult.Success)?.data.orEmpty() }
                val timetableJob = async { (timetableRepository.getTimetable() as? ApiResult.Success)?.data.orEmpty() }
                val examsJob = async { (examRepository.getExams() as? ApiResult.Success)?.data.orEmpty() }
                val sessionsJob = async { (studyRepository.getSessions() as? ApiResult.Success)?.data.orEmpty() }

                val modules = modulesJob.await()
                val exams = examsJob.await()
                val timetable = timetableJob.await()
                val sessions = sessionsJob.await()
                if (_binding == null) return@coroutineScope

                bindNextClass(timetable, modules)
                bindExamsAndProgress(exams, modules)
                bindStudyToday(sessions)
                bindPriorityTasks(modules, exams)
                binding.swipeRefresh.isRefreshing = false
            }
        }
    }

    private fun bindNextClass(entries: List<TimetableEntry>, modules: List<Module>) {
        val today = DateUtils.todayStoredDayName()
        val now = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"))
        val next = entries.filter { it.dayOfWeek == today && it.endTime >= now }.minByOrNull { it.startTime }

        if (next == null) {
            binding.textNextClass.text = getString(R.string.dashboard_no_class_today)
            binding.textNextClassDetail.text = ""
        } else {
            binding.textNextClass.text = modules.find { it.id == next.moduleId }?.name ?: getString(R.string.unknown_module)
            binding.textNextClassDetail.text = listOf("${next.startTime} – ${next.endTime}", next.venue)
                .filter { it.isNotBlank() }.joinToString(" · ")
        }
    }

    private fun bindExamsAndProgress(exams: List<Exam>, modules: List<Module>) {
        val nextExam = exams.filter { (DateUtils.daysUntil(it.date) ?: -1) >= 0 }.minByOrNull { it.date + it.time }
        if (nextExam == null) {
            binding.textNextExam.text = getString(R.string.dashboard_no_exams)
            binding.textNextExamDetail.text = ""
        } else {
            val module = modules.find { it.id == nextExam.moduleId }?.code
            binding.textNextExam.text = nextExam.title
            binding.textNextExamDetail.text = listOfNotNull(
                module?.ifBlank { null },
                requireContext().displayDate(nextExam.date),
                requireContext().relativeDays(nextExam.date)
            ).joinToString(" · ")
        }

        val average = StudyPlanner.averageMark(exams)
        if (average == null) {
            binding.textProgressValue.text = "–"
            binding.textProgressDetail.text = getString(R.string.dashboard_progress_empty)
            binding.progressAverage.progress = 0
        } else {
            val marked = exams.count { it.mark != null }
            binding.textProgressValue.text = getString(R.string.percent_value, average.roundToInt())
            binding.textProgressDetail.text = resources.getQuantityString(R.plurals.dashboard_progress_detail, marked, marked)
            binding.progressAverage.progress = average.roundToInt()
        }
    }

    private fun bindStudyToday(sessions: List<StudySession>) {
        val today = sessions.filter { it.date == DateUtils.todayIso() && !it.completed }
        binding.textStudyToday.text = if (today.isEmpty()) {
            getString(R.string.dashboard_study_none)
        } else {
            resources.getQuantityString(R.plurals.dashboard_study_today, today.size, today.size, today.sumOf { it.durationMinutes })
        }
    }

    private suspend fun bindPriorityTasks(modules: List<Module>, exams: List<Exam>) {
        val tasks = (taskRepository.getTasks() as? ApiResult.Success)?.data.orEmpty()
        val examModules = PriorityUtils.modulesWithExamSoon(exams)
        adapter.setModules(modules)
        adapter.setExamModules(examModules)
        val top = PriorityUtils.topPriorityTasks(tasks, exams = exams)
        adapter.submitList(top)
        binding.textNoTasks.visibility = if (top.isEmpty()) View.VISIBLE else View.GONE
        Log.d(tag, "Dashboard shows ${top.size} priority tasks of ${tasks.size}")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

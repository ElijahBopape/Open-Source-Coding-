package com.bopape.studentassistant.ui.study

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
import com.bopape.studentassistant.data.model.Exam
import com.bopape.studentassistant.data.model.StudySession
import com.bopape.studentassistant.data.model.Task
import com.bopape.studentassistant.data.repository.ExamRepository
import com.bopape.studentassistant.data.repository.StudySessionRepository
import com.bopape.studentassistant.data.repository.TaskRepository
import com.bopape.studentassistant.databinding.FragmentStudyPlannerBinding
import com.bopape.studentassistant.util.ApiResult
import com.bopape.studentassistant.util.DateUtils
import com.bopape.studentassistant.util.StudyPlanner
import com.bopape.studentassistant.util.displayDate
import kotlinx.coroutines.launch

/**
 * The Study Planner (User Defined feature 5 and the second innovative feature in
 * the Part 1 design). Students schedule study sessions linked to exams or tasks,
 * tick them off, and see their progress. "Smart plan" generates a set of evenly
 * spaced sessions for a chosen exam or task automatically.
 */
class StudyPlannerFragment : Fragment() {

    private var _binding: FragmentStudyPlannerBinding? = null
    private val binding get() = _binding!!

    private val studyRepository = StudySessionRepository()
    private val examRepository = ExamRepository()
    private val taskRepository = TaskRepository()
    private lateinit var adapter: StudySessionAdapter

    private var sessions: List<StudySession> = emptyList()
    private var exams: List<Exam> = emptyList()
    private var tasks: List<Task> = emptyList()
    private var showCompleted = false
    private val tag = "StudyPlannerFragment"

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentStudyPlannerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = StudySessionAdapter(
            onClick = { startActivity(AddEditStudySessionActivity.editIntent(requireContext(), it)) },
            onToggle = { session, done -> updateSession(session.copy(completed = done)) },
            onDelete = { confirmDelete(it) }
        )
        binding.recyclerSessions.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerSessions.adapter = adapter

        binding.chipGroupStudyFilter.setOnCheckedStateChangeListener { _, ids ->
            showCompleted = ids.firstOrNull() == R.id.chipStudyDone
            applyFilter()
        }
        binding.buttonSmartPlan.setOnClickListener { chooseSmartPlanTarget() }
        binding.fabAddSession.setOnClickListener {
            startActivity(Intent(requireContext(), AddEditStudySessionActivity::class.java))
        }
        binding.swipeRefresh.setOnRefreshListener { load() }
    }

    override fun onResume() {
        super.onResume()
        load()
    }

    private fun load() {
        viewLifecycleOwner.lifecycleScope.launch {
            exams = (examRepository.getExams() as? ApiResult.Success)?.data.orEmpty()
            tasks = (taskRepository.getTasks() as? ApiResult.Success)?.data.orEmpty()
            adapter.setLinks(exams, tasks)

            when (val result = studyRepository.getSessions()) {
                is ApiResult.Success -> {
                    sessions = result.data
                    Log.d(tag, "Loaded ${sessions.size} study sessions (fromCache=${result.fromCache})")
                    applyFilter()
                }
                is ApiResult.Failure -> Toast.makeText(requireContext(), result.message, Toast.LENGTH_SHORT).show()
            }
            _binding?.swipeRefresh?.isRefreshing = false
        }
    }

    private fun applyFilter() {
        if (_binding == null) return
        val visible = sessions.filter { it.completed == showCompleted }
            .let { list -> if (showCompleted) list.sortedByDescending { it.date } else list.sortedBy { it.date + it.startTime } }
        adapter.submitList(visible)
        binding.textNoSessions.visibility = if (visible.isEmpty()) View.VISIBLE else View.GONE

        val percent = StudyPlanner.completionPercent(sessions)
        binding.progressStudy.progress = percent
        binding.textStudyProgress.text = getString(R.string.study_progress, percent, sessions.count { it.completed }, sessions.size)
        val minutes = StudyPlanner.remainingMinutes(sessions)
        binding.textStudyRemaining.text = getString(R.string.study_remaining, minutes / 60, minutes % 60)
    }

    private fun updateSession(session: StudySession) {
        viewLifecycleOwner.lifecycleScope.launch {
            when (val result = studyRepository.updateSession(session)) {
                is ApiResult.Success -> load()
                is ApiResult.Failure -> {
                    Toast.makeText(requireContext(), result.message, Toast.LENGTH_SHORT).show()
                    load()
                }
            }
        }
    }

    private fun confirmDelete(session: StudySession) {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.delete_session_confirm_title)
            .setMessage(session.title)
            .setPositiveButton(R.string.delete) { _, _ ->
                viewLifecycleOwner.lifecycleScope.launch {
                    studyRepository.deleteSession(session.id)
                    load()
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    /** Step 1 of Smart plan: pick an upcoming exam or a pending task to plan for. */
    private fun chooseSmartPlanTarget() {
        val upcomingExams = exams.filter { (DateUtils.daysUntil(it.date) ?: -1) > 0 }
        val openTasks = tasks.filter { !it.completed && it.remoteId != null && (DateUtils.daysUntil(it.dueDate) ?: -1) > 0 }
        if (upcomingExams.isEmpty() && openTasks.isEmpty()) {
            Toast.makeText(requireContext(), R.string.smart_plan_nothing, Toast.LENGTH_LONG).show()
            return
        }
        val labels = upcomingExams.map { getString(R.string.smart_plan_exam_item, it.title, requireContext().displayDate(it.date)) } +
            openTasks.map { getString(R.string.smart_plan_task_item, it.title, requireContext().displayDate(it.dueDate)) }

        AlertDialog.Builder(requireContext())
            .setTitle(R.string.smart_plan_choose)
            .setItems(labels.toTypedArray()) { _, which ->
                if (which < upcomingExams.size) {
                    val exam = upcomingExams[which]
                    chooseSessionCount { count ->
                        savePlan(StudyPlanner.planForExam(exam, DateUtils.today(), count, 60, getString(R.string.plan_session_prefix)))
                    }
                } else {
                    val task = openTasks[which - upcomingExams.size]
                    chooseSessionCount { count ->
                        savePlan(
                            StudyPlanner.suggestSessions(
                                title = getString(R.string.plan_work_prefix, task.title),
                                deadline = DateUtils.parse(task.dueDate) ?: return@chooseSessionCount,
                                today = DateUtils.today(),
                                sessionCount = count,
                                moduleId = task.moduleId,
                                taskId = task.remoteId
                            )
                        )
                    }
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    /** Step 2 of Smart plan: how many sessions. */
    private fun chooseSessionCount(onChosen: (Int) -> Unit) {
        val options = intArrayOf(2, 3, 4, 6)
        val labels = options.map { resources.getQuantityString(R.plurals.plan_option, it, it) }.toTypedArray()
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.create_plan_title)
            .setItems(labels) { _, which -> onChosen(options[which]) }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun savePlan(plan: List<StudySession>) {
        if (plan.isEmpty()) {
            Toast.makeText(requireContext(), R.string.plan_no_time, Toast.LENGTH_SHORT).show()
            return
        }
        viewLifecycleOwner.lifecycleScope.launch {
            when (val result = studyRepository.createAll(plan)) {
                is ApiResult.Success -> Toast.makeText(
                    requireContext(),
                    resources.getQuantityString(R.plurals.plan_created, result.data, result.data),
                    Toast.LENGTH_LONG
                ).show()
                is ApiResult.Failure -> Toast.makeText(requireContext(), result.message, Toast.LENGTH_LONG).show()
            }
            load()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

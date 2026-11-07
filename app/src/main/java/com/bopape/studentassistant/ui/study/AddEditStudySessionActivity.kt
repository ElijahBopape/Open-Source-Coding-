package com.bopape.studentassistant.ui.study

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.bopape.studentassistant.R
import com.bopape.studentassistant.data.model.Exam
import com.bopape.studentassistant.data.model.StudySession
import com.bopape.studentassistant.data.model.Task
import com.bopape.studentassistant.data.repository.ExamRepository
import com.bopape.studentassistant.data.repository.StudySessionRepository
import com.bopape.studentassistant.data.repository.TaskRepository
import com.bopape.studentassistant.databinding.ActivityAddEditStudySessionBinding
import com.bopape.studentassistant.util.ApiResult
import com.bopape.studentassistant.util.DateUtils
import com.bopape.studentassistant.util.applySystemBarInsets
import com.bopape.studentassistant.util.displayDate
import kotlinx.coroutines.launch
import java.util.Locale

/** Creates or edits a single study session, optionally linked to an exam or a task. */
class AddEditStudySessionActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddEditStudySessionBinding
    private val studyRepository = StudySessionRepository()
    private val examRepository = ExamRepository()
    private val taskRepository = TaskRepository()

    private var editing: StudySession? = null
    private var exams: List<Exam> = emptyList()
    private var tasks: List<Task> = emptyList()
    private var selectedDate = DateUtils.todayIso()
    private var selectedTime = "18:00"
    private val tag = "AddEditStudySession"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddEditStudySessionBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.root.applySystemBarInsets()
        binding.toolbar.setNavigationOnClickListener { finish() }

        val id = intent.getIntExtra(EXTRA_ID, 0)
        if (id != 0) {
            editing = StudySession(
                id = id,
                title = intent.getStringExtra(EXTRA_TITLE).orEmpty(),
                date = intent.getStringExtra(EXTRA_DATE) ?: selectedDate,
                startTime = intent.getStringExtra(EXTRA_TIME).orEmpty(),
                durationMinutes = intent.getIntExtra(EXTRA_DURATION, 60),
                moduleId = intent.getIntExtra(EXTRA_MODULE_ID, -1).takeIf { it > 0 },
                examId = intent.getIntExtra(EXTRA_EXAM_ID, -1).takeIf { it > 0 },
                taskId = intent.getIntExtra(EXTRA_TASK_ID, -1).takeIf { it > 0 },
                completed = intent.getBooleanExtra(EXTRA_COMPLETED, false)
            )
        }
        editing?.let {
            binding.toolbar.title = getString(R.string.edit_session_title)
            binding.editSessionTitle.setText(it.title)
            binding.editSessionDuration.setText(it.durationMinutes.toString())
            selectedDate = it.date
            selectedTime = it.startTime.ifBlank { selectedTime }
        }
        binding.editSessionDate.setText(displayDate(selectedDate))
        binding.editSessionTime.setText(selectedTime)
        binding.editSessionDate.setOnClickListener { pickDate() }
        binding.editSessionTime.setOnClickListener { pickTime() }
        binding.buttonSaveSession.setOnClickListener { save() }

        loadLinkOptions()
    }

    /** "Preparing for" spinner: None, then upcoming exams, then pending (synced) tasks. */
    private fun loadLinkOptions() {
        lifecycleScope.launch {
            exams = (examRepository.getExams() as? ApiResult.Success)?.data.orEmpty()
            tasks = (taskRepository.getTasks() as? ApiResult.Success)?.data.orEmpty().filter { it.remoteId != null }
            val labels = listOf(getString(R.string.none_option)) +
                exams.map { getString(R.string.linked_exam, it.title) } +
                tasks.map { getString(R.string.linked_task, it.title) }
            binding.spinnerSessionLink.adapter =
                ArrayAdapter(this@AddEditStudySessionActivity, android.R.layout.simple_spinner_dropdown_item, labels)

            val current = editing
            val index = when {
                current?.examId != null -> exams.indexOfFirst { it.id == current.examId }.let { if (it >= 0) it + 1 else 0 }
                current?.taskId != null -> tasks.indexOfFirst { it.remoteId == current.taskId }.let { if (it >= 0) it + 1 + exams.size else 0 }
                else -> 0
            }
            binding.spinnerSessionLink.setSelection(index)
        }
    }

    private fun pickDate() {
        val current = DateUtils.parse(selectedDate) ?: DateUtils.today()
        DatePickerDialog(this, { _, y, m, d ->
            selectedDate = DateUtils.isoDateFrom(y, m, d)
            binding.editSessionDate.setText(displayDate(selectedDate))
        }, current.year, current.monthValue - 1, current.dayOfMonth).show()
    }

    private fun pickTime() {
        val (h, m) = selectedTime.split(":").map { it.toIntOrNull() ?: 0 }
        TimePickerDialog(this, { _, hour, minute ->
            selectedTime = String.format(Locale.US, "%02d:%02d", hour, minute)
            binding.editSessionTime.setText(selectedTime)
        }, h, m, true).show()
    }

    private fun save() {
        val title = binding.editSessionTitle.text.toString().trim()
        binding.layoutSessionTitle.error = if (title.isEmpty()) getString(R.string.error_session_title_required) else null
        val duration = binding.editSessionDuration.text.toString().trim().toIntOrNull()
        binding.layoutSessionDuration.error = if (duration == null || duration !in 5..600) getString(R.string.error_duration_range) else null
        if (binding.layoutSessionTitle.error != null || binding.layoutSessionDuration.error != null) return

        val linkIndex = binding.spinnerSessionLink.selectedItemPosition
        val exam = exams.getOrNull(linkIndex - 1)
        val task = if (exam == null) tasks.getOrNull(linkIndex - 1 - exams.size) else null

        val session = StudySession(
            id = editing?.id ?: 0,
            title = title,
            date = selectedDate,
            startTime = selectedTime,
            durationMinutes = duration!!,
            moduleId = exam?.moduleId ?: task?.moduleId,
            examId = exam?.id,
            taskId = task?.remoteId,
            completed = editing?.completed ?: false
        )

        setLoading(true)
        lifecycleScope.launch {
            val result = if (session.id == 0) studyRepository.createSession(session) else studyRepository.updateSession(session)
            setLoading(false)
            when (result) {
                is ApiResult.Success -> {
                    Log.i(tag, "Saved study session '${session.title}'")
                    finish()
                }
                is ApiResult.Failure -> Toast.makeText(this@AddEditStudySessionActivity, result.message, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun setLoading(loading: Boolean) {
        binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        binding.buttonSaveSession.isEnabled = !loading
    }

    companion object {
        private const val EXTRA_ID = "session_id"
        private const val EXTRA_TITLE = "session_title"
        private const val EXTRA_DATE = "session_date"
        private const val EXTRA_TIME = "session_time"
        private const val EXTRA_DURATION = "session_duration"
        private const val EXTRA_MODULE_ID = "session_module_id"
        private const val EXTRA_EXAM_ID = "session_exam_id"
        private const val EXTRA_TASK_ID = "session_task_id"
        private const val EXTRA_COMPLETED = "session_completed"

        fun editIntent(context: Context, s: StudySession) = Intent(context, AddEditStudySessionActivity::class.java).apply {
            putExtra(EXTRA_ID, s.id)
            putExtra(EXTRA_TITLE, s.title)
            putExtra(EXTRA_DATE, s.date)
            putExtra(EXTRA_TIME, s.startTime)
            putExtra(EXTRA_DURATION, s.durationMinutes)
            putExtra(EXTRA_MODULE_ID, s.moduleId ?: -1)
            putExtra(EXTRA_EXAM_ID, s.examId ?: -1)
            putExtra(EXTRA_TASK_ID, s.taskId ?: -1)
            putExtra(EXTRA_COMPLETED, s.completed)
        }
    }
}

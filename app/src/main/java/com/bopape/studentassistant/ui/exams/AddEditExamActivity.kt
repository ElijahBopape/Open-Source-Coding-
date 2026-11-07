package com.bopape.studentassistant.ui.exams

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.bopape.studentassistant.R
import com.bopape.studentassistant.data.model.Exam
import com.bopape.studentassistant.data.model.Module
import com.bopape.studentassistant.data.repository.ExamRepository
import com.bopape.studentassistant.data.repository.ModuleRepository
import com.bopape.studentassistant.data.repository.StudySessionRepository
import com.bopape.studentassistant.databinding.ActivityAddEditExamBinding
import com.bopape.studentassistant.util.ApiResult
import com.bopape.studentassistant.util.DateUtils
import com.bopape.studentassistant.util.StudyPlanner
import com.bopape.studentassistant.util.applySystemBarInsets
import com.bopape.studentassistant.util.displayDate
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * Creates or edits an exam. Validates every field before saving (title, date,
 * duration and an optional 0-100 mark), and for a saved upcoming exam offers
 * "Create study plan", which uses [StudyPlanner] to generate revision sessions.
 */
class AddEditExamActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddEditExamBinding
    private val examRepository = ExamRepository()
    private val moduleRepository = ModuleRepository()
    private val studyRepository = StudySessionRepository()

    private var editingExam: Exam? = null
    private var modules: List<Module> = emptyList()
    private var selectedDate: String = DateUtils.today().plusDays(14).toString()
    private var selectedTime: String = "09:00"
    private val tag = "AddEditExamActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddEditExamBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.root.applySystemBarInsets()
        binding.toolbar.setNavigationOnClickListener { finish() }

        val id = intent.getIntExtra(EXTRA_ID, 0)
        if (id != 0) {
            editingExam = Exam(
                id = id,
                title = intent.getStringExtra(EXTRA_TITLE).orEmpty(),
                moduleId = intent.getIntExtra(EXTRA_MODULE_ID, -1).takeIf { it > 0 },
                date = intent.getStringExtra(EXTRA_DATE) ?: selectedDate,
                time = intent.getStringExtra(EXTRA_TIME).orEmpty(),
                venue = intent.getStringExtra(EXTRA_VENUE).orEmpty(),
                durationMinutes = intent.getIntExtra(EXTRA_DURATION, 120),
                mark = if (intent.hasExtra(EXTRA_MARK)) intent.getDoubleExtra(EXTRA_MARK, 0.0) else null
            )
        }

        editingExam?.let { exam ->
            binding.toolbar.title = getString(R.string.edit_exam_title)
            binding.editExamTitle.setText(exam.title)
            binding.editExamVenue.setText(exam.venue)
            binding.editExamDuration.setText(exam.durationMinutes.toString())
            exam.mark?.let { binding.editExamMark.setText(String.format(Locale.US, "%.0f", it)) }
            selectedDate = exam.date
            selectedTime = exam.time.ifBlank { selectedTime }
            val upcoming = (DateUtils.daysUntil(exam.date) ?: -1) > 0
            binding.buttonCreatePlan.visibility = if (upcoming) View.VISIBLE else View.GONE
        }

        binding.editExamDate.setText(displayDate(selectedDate))
        binding.editExamTime.setText(selectedTime)
        binding.editExamDate.setOnClickListener { pickDate() }
        binding.editExamTime.setOnClickListener { pickTime() }
        binding.buttonSaveExam.setOnClickListener { save() }
        binding.buttonCreatePlan.setOnClickListener { showPlanDialog() }

        loadModules()
    }

    private fun loadModules() {
        lifecycleScope.launch {
            modules = (moduleRepository.getModules() as? ApiResult.Success)?.data.orEmpty()
            val names = listOf(getString(R.string.none_option)) + modules.map { if (it.code.isBlank()) it.name else "${it.name} (${it.code})" }
            binding.spinnerExamModule.adapter = ArrayAdapter(this@AddEditExamActivity, android.R.layout.simple_spinner_dropdown_item, names)
            val index = modules.indexOfFirst { it.id == editingExam?.moduleId }
            if (index >= 0) binding.spinnerExamModule.setSelection(index + 1)
        }
    }

    private fun pickDate() {
        val current = DateUtils.parse(selectedDate) ?: DateUtils.today()
        DatePickerDialog(this, { _, y, m, d ->
            selectedDate = DateUtils.isoDateFrom(y, m, d)
            binding.editExamDate.setText(displayDate(selectedDate))
        }, current.year, current.monthValue - 1, current.dayOfMonth).show()
    }

    private fun pickTime() {
        val (h, m) = selectedTime.split(":").map { it.toIntOrNull() ?: 0 }
        TimePickerDialog(this, { _, hour, minute ->
            selectedTime = String.format(Locale.US, "%02d:%02d", hour, minute)
            binding.editExamTime.setText(selectedTime)
        }, h, m, true).show()
    }

    /** Validates the form; returns the exam to save, or null after showing field errors. */
    private fun readForm(): Exam? {
        val title = binding.editExamTitle.text.toString().trim()
        binding.layoutExamTitle.error = if (title.isEmpty()) getString(R.string.error_exam_title_required) else null

        val durationText = binding.editExamDuration.text.toString().trim()
        val duration = durationText.toIntOrNull()
        binding.layoutExamDuration.error =
            if (durationText.isNotEmpty() && (duration == null || duration !in 1..600)) getString(R.string.error_duration_range) else null

        val markText = binding.editExamMark.text.toString().trim().replace(',', '.')
        val mark = markText.toDoubleOrNull()
        binding.layoutExamMark.error =
            if (markText.isNotEmpty() && (mark == null || mark !in 0.0..100.0)) getString(R.string.error_mark_range) else null

        if (binding.layoutExamTitle.error != null || binding.layoutExamDuration.error != null || binding.layoutExamMark.error != null) {
            return null
        }

        val modulePosition = binding.spinnerExamModule.selectedItemPosition
        return Exam(
            id = editingExam?.id ?: 0,
            title = title,
            moduleId = if (modulePosition in 1..modules.size) modules[modulePosition - 1].id else null,
            date = selectedDate,
            time = selectedTime,
            venue = binding.editExamVenue.text.toString().trim(),
            durationMinutes = duration ?: 120,
            mark = if (markText.isEmpty()) null else mark
        )
    }

    private fun save() {
        val exam = readForm() ?: return
        setLoading(true)
        lifecycleScope.launch {
            val result = if (exam.id == 0) examRepository.createExam(exam) else examRepository.updateExam(exam)
            setLoading(false)
            when (result) {
                is ApiResult.Success -> {
                    Log.i(tag, "Saved exam #${result.data.id} '${exam.title}'")
                    finish()
                }
                is ApiResult.Failure -> Toast.makeText(this@AddEditExamActivity, result.message, Toast.LENGTH_LONG).show()
            }
        }
    }

    /** Smart plan: choose how many sessions, then generate and save them. */
    private fun showPlanDialog() {
        val exam = editingExam ?: return
        val options = arrayOf(3, 4, 6, 8)
        val labels = options.map { resources.getQuantityString(R.plurals.plan_option, it, it) }.toTypedArray()
        AlertDialog.Builder(this)
            .setTitle(R.string.create_plan_title)
            .setItems(labels) { _, which -> createPlan(exam, options[which]) }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun createPlan(exam: Exam, count: Int) {
        val sessions = StudyPlanner.planForExam(exam, DateUtils.today(), count, 60, getString(R.string.plan_session_prefix))
        if (sessions.isEmpty()) {
            Toast.makeText(this, R.string.plan_no_time, Toast.LENGTH_SHORT).show()
            return
        }
        setLoading(true)
        lifecycleScope.launch {
            val result = studyRepository.createAll(sessions)
            setLoading(false)
            when (result) {
                is ApiResult.Success -> Toast.makeText(
                    this@AddEditExamActivity,
                    resources.getQuantityString(R.plurals.plan_created, result.data, result.data),
                    Toast.LENGTH_LONG
                ).show()
                is ApiResult.Failure -> Toast.makeText(this@AddEditExamActivity, result.message, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun setLoading(loading: Boolean) {
        binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        binding.buttonSaveExam.isEnabled = !loading
        binding.buttonCreatePlan.isEnabled = !loading
    }

    companion object {
        private const val EXTRA_ID = "exam_id"
        private const val EXTRA_TITLE = "exam_title"
        private const val EXTRA_MODULE_ID = "exam_module_id"
        private const val EXTRA_DATE = "exam_date"
        private const val EXTRA_TIME = "exam_time"
        private const val EXTRA_VENUE = "exam_venue"
        private const val EXTRA_DURATION = "exam_duration"
        private const val EXTRA_MARK = "exam_mark"

        fun editIntent(context: Context, exam: Exam) = Intent(context, AddEditExamActivity::class.java).apply {
            putExtra(EXTRA_ID, exam.id)
            putExtra(EXTRA_TITLE, exam.title)
            putExtra(EXTRA_MODULE_ID, exam.moduleId ?: -1)
            putExtra(EXTRA_DATE, exam.date)
            putExtra(EXTRA_TIME, exam.time)
            putExtra(EXTRA_VENUE, exam.venue)
            putExtra(EXTRA_DURATION, exam.durationMinutes)
            exam.mark?.let { putExtra(EXTRA_MARK, it) }
        }
    }
}

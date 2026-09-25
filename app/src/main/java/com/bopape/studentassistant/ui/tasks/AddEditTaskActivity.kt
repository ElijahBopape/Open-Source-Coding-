package com.bopape.studentassistant.ui.tasks

import android.app.DatePickerDialog
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.bopape.studentassistant.R
import com.bopape.studentassistant.data.model.Module
import com.bopape.studentassistant.data.model.Priority
import com.bopape.studentassistant.data.model.Task
import com.bopape.studentassistant.data.repository.ModuleRepository
import com.bopape.studentassistant.data.repository.TaskRepository
import com.bopape.studentassistant.databinding.ActivityAddEditTaskBinding
import com.bopape.studentassistant.util.ApiResult
import com.bopape.studentassistant.util.DateUtils
import kotlinx.coroutines.launch
import java.util.Calendar

/** Creates a new task, or edits an existing one when launched with EXTRA_TASK_ID. */
class AddEditTaskActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddEditTaskBinding
    private val taskRepository = TaskRepository()
    private val moduleRepository = ModuleRepository()

    private var editingTaskId: Int = 0
    private var isCompleted: Boolean = false
    private var modules: List<Module> = emptyList()
    private var selectedDueDate: String = DateUtils.todayIso()
    private val tag = "AddEditTaskActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddEditTaskBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toolbar.setNavigationOnClickListener { finish() }
        binding.editDueDate.setText(DateUtils.formatIsoDateForDisplay(selectedDueDate))
        binding.editDueDate.setOnClickListener { showDatePicker() }

        editingTaskId = intent.getIntExtra(EXTRA_TASK_ID, 0)
        if (editingTaskId != 0) {
            binding.toolbar.title = getString(R.string.edit_task_title)
            binding.editTaskTitle.setText(intent.getStringExtra(EXTRA_TASK_TITLE))
            binding.editTaskDescription.setText(intent.getStringExtra(EXTRA_TASK_DESCRIPTION))
            selectedDueDate = intent.getStringExtra(EXTRA_TASK_DUE_DATE) ?: selectedDueDate
            binding.editDueDate.setText(DateUtils.formatIsoDateForDisplay(selectedDueDate))
            isCompleted = intent.getBooleanExtra(EXTRA_TASK_COMPLETED, false)
            when (intent.getStringExtra(EXTRA_TASK_PRIORITY)) {
                Priority.HIGH -> binding.radioHigh.isChecked = true
                Priority.LOW -> binding.radioLow.isChecked = true
                else -> binding.radioMedium.isChecked = true
            }
        }

        loadModules()
        binding.buttonSaveTask.setOnClickListener { saveTask() }
    }

    private fun loadModules() {
        lifecycleScope.launch {
            when (val result = moduleRepository.getModules()) {
                is ApiResult.Success -> {
                    modules = result.data
                    val names = mutableListOf("None")
                    names.addAll(modules.map { "${it.name} (${it.code})" })
                    binding.spinnerModule.adapter = ArrayAdapter(
                        this@AddEditTaskActivity,
                        android.R.layout.simple_spinner_dropdown_item,
                        names
                    )
                    val editingModuleId = intent.getIntExtra(EXTRA_TASK_MODULE_ID, -1)
                    val index = modules.indexOfFirst { it.id == editingModuleId }
                    if (index >= 0) binding.spinnerModule.setSelection(index + 1)
                }
                is ApiResult.Failure -> Log.w(tag, "Could not load modules for task form: ${result.message}")
            }
        }
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                selectedDueDate = DateUtils.isoDateFrom(year, month, dayOfMonth)
                binding.editDueDate.setText(DateUtils.formatIsoDateForDisplay(selectedDueDate))
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun saveTask() {
        val title = binding.editTaskTitle.text.toString().trim()
        if (title.isEmpty()) {
            binding.editTaskTitle.error = getString(R.string.error_task_title_required)
            return
        }

        val selectedModulePosition = binding.spinnerModule.selectedItemPosition
        val moduleId = if (selectedModulePosition in 1..modules.size) {
            modules[selectedModulePosition - 1].id
        } else null

        val priority = when (binding.radioGroupPriority.checkedRadioButtonId) {
            R.id.radioHigh -> Priority.HIGH
            R.id.radioLow -> Priority.LOW
            else -> Priority.MEDIUM
        }

        val task = Task(
            id = editingTaskId,
            title = title,
            description = binding.editTaskDescription.text.toString().trim(),
            moduleId = moduleId,
            dueDate = selectedDueDate,
            priority = priority,
            completed = isCompleted
        )

        setLoading(true)
        lifecycleScope.launch {
            val result = if (editingTaskId == 0) {
                Log.d(tag, "Creating task '$title'")
                taskRepository.createTask(task)
            } else {
                Log.d(tag, "Updating task #$editingTaskId")
                taskRepository.updateTask(task)
            }
            setLoading(false)
            when (result) {
                is ApiResult.Success -> finish()
                is ApiResult.Failure -> Toast.makeText(this@AddEditTaskActivity, result.message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setLoading(loading: Boolean) {
        binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        binding.buttonSaveTask.isEnabled = !loading
    }

    companion object {
        const val EXTRA_TASK_ID = "extra_task_id"
        const val EXTRA_TASK_TITLE = "extra_task_title"
        const val EXTRA_TASK_DESCRIPTION = "extra_task_description"
        const val EXTRA_TASK_MODULE_ID = "extra_task_module_id"
        const val EXTRA_TASK_DUE_DATE = "extra_task_due_date"
        const val EXTRA_TASK_PRIORITY = "extra_task_priority"
        const val EXTRA_TASK_COMPLETED = "extra_task_completed"
    }
}

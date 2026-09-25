package com.bopape.studentassistant.ui.timetable

import android.app.TimePickerDialog
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.bopape.studentassistant.R
import com.bopape.studentassistant.data.model.Module
import com.bopape.studentassistant.data.model.TimetableEntry
import com.bopape.studentassistant.data.repository.ModuleRepository
import com.bopape.studentassistant.data.repository.TimetableRepository
import com.bopape.studentassistant.databinding.ActivityAddEditTimetableBinding
import com.bopape.studentassistant.util.ApiResult
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Locale

/** Creates a new class entry, or edits an existing one when launched with EXTRA_ENTRY_ID. */
class AddEditTimetableActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddEditTimetableBinding
    private val timetableRepository = TimetableRepository()
    private val moduleRepository = ModuleRepository()

    private var editingEntryId: Int = 0
    private var modules: List<Module> = emptyList()
    private var startTime: String = "09:00"
    private var endTime: String = "10:00"
    private val tag = "AddEditTimetableActivity"

    private val daysOfWeek = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddEditTimetableBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toolbar.setNavigationOnClickListener { finish() }
        binding.editStartTime.setText(startTime)
        binding.editEndTime.setText(endTime)
        binding.editStartTime.setOnClickListener { showTimePicker(isStart = true) }
        binding.editEndTime.setOnClickListener { showTimePicker(isStart = false) }

        editingEntryId = intent.getIntExtra(EXTRA_ENTRY_ID, 0)
        if (editingEntryId != 0) {
            binding.toolbar.title = getString(R.string.edit_class_title)
            startTime = intent.getStringExtra(EXTRA_ENTRY_START) ?: startTime
            endTime = intent.getStringExtra(EXTRA_ENTRY_END) ?: endTime
            binding.editStartTime.setText(startTime)
            binding.editEndTime.setText(endTime)
            binding.editVenue.setText(intent.getStringExtra(EXTRA_ENTRY_VENUE))

            val day = intent.getStringExtra(EXTRA_ENTRY_DAY)
            val dayIndex = daysOfWeek.indexOf(day)
            if (dayIndex >= 0) binding.spinnerDay.setSelection(dayIndex)
        }

        loadModules()
        binding.buttonSaveClass.setOnClickListener { saveEntry() }
    }

    private fun loadModules() {
        lifecycleScope.launch {
            when (val result = moduleRepository.getModules()) {
                is ApiResult.Success -> {
                    modules = result.data
                    binding.spinnerModule.adapter = ArrayAdapter(
                        this@AddEditTimetableActivity,
                        android.R.layout.simple_spinner_dropdown_item,
                        modules.map { "${it.name} (${it.code})" }
                    )
                    val editingModuleId = intent.getIntExtra(EXTRA_ENTRY_MODULE_ID, -1)
                    val index = modules.indexOfFirst { it.id == editingModuleId }
                    if (index >= 0) binding.spinnerModule.setSelection(index)
                }
                is ApiResult.Failure -> Log.w(tag, "Could not load modules for timetable form: ${result.message}")
            }
        }
    }

    private fun showTimePicker(isStart: Boolean) {
        val calendar = Calendar.getInstance()
        TimePickerDialog(
            this,
            { _, hourOfDay, minute ->
                val formatted = String.format(Locale.getDefault(), "%02d:%02d", hourOfDay, minute)
                if (isStart) {
                    startTime = formatted
                    binding.editStartTime.setText(formatted)
                } else {
                    endTime = formatted
                    binding.editEndTime.setText(formatted)
                }
            },
            calendar.get(Calendar.HOUR_OF_DAY),
            calendar.get(Calendar.MINUTE),
            true
        ).show()
    }

    private fun saveEntry() {
        if (modules.isEmpty()) {
            Toast.makeText(this, R.string.error_module_required, Toast.LENGTH_SHORT).show()
            return
        }
        if (endTime <= startTime) {
            Toast.makeText(this, R.string.error_end_before_start, Toast.LENGTH_SHORT).show()
            return
        }

        val module = modules[binding.spinnerModule.selectedItemPosition]
        val day = daysOfWeek[binding.spinnerDay.selectedItemPosition]

        val entry = TimetableEntry(
            id = editingEntryId,
            moduleId = module.id,
            dayOfWeek = day,
            startTime = startTime,
            endTime = endTime,
            venue = binding.editVenue.text.toString().trim()
        )

        setLoading(true)
        lifecycleScope.launch {
            val result = if (editingEntryId == 0) {
                Log.d(tag, "Creating timetable entry for module #${module.id} on $day")
                timetableRepository.createEntry(entry)
            } else {
                Log.d(tag, "Updating timetable entry #$editingEntryId")
                timetableRepository.updateEntry(entry)
            }
            setLoading(false)
            when (result) {
                is ApiResult.Success -> finish()
                is ApiResult.Failure -> Toast.makeText(this@AddEditTimetableActivity, result.message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setLoading(loading: Boolean) {
        binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        binding.buttonSaveClass.isEnabled = !loading
    }

    companion object {
        const val EXTRA_ENTRY_ID = "extra_entry_id"
        const val EXTRA_ENTRY_MODULE_ID = "extra_entry_module_id"
        const val EXTRA_ENTRY_DAY = "extra_entry_day"
        const val EXTRA_ENTRY_START = "extra_entry_start"
        const val EXTRA_ENTRY_END = "extra_entry_end"
        const val EXTRA_ENTRY_VENUE = "extra_entry_venue"
    }
}

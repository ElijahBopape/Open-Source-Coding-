package com.bopape.studentassistant.ui.timetable

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
import com.bopape.studentassistant.data.model.TimetableEntry
import com.bopape.studentassistant.data.repository.ModuleRepository
import com.bopape.studentassistant.data.repository.TimetableRepository
import com.bopape.studentassistant.databinding.FragmentTimetableBinding
import com.bopape.studentassistant.util.ApiResult
import com.google.android.material.chip.Chip
import kotlinx.coroutines.launch
import java.util.Calendar

/** Shows the weekly class schedule, one day at a time, selected via the day chips. */
class TimetableFragment : Fragment() {

    private var _binding: FragmentTimetableBinding? = null
    private val binding get() = _binding!!

    private val timetableRepository = TimetableRepository()
    private val moduleRepository = ModuleRepository()
    private lateinit var adapter: TimetableAdapter

    private var allEntries: List<TimetableEntry> = emptyList()
    private var selectedDay: String = currentDayOfWeek()
    private val tag = "TimetableFragment"

    private val daysOfWeek = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTimetableBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = TimetableAdapter(
            onClick = { openEditor(it) },
            onDelete = { confirmDelete(it) }
        )
        binding.recyclerTimetable.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerTimetable.adapter = adapter

        setUpDayChips()

        binding.fabAddClass.setOnClickListener {
            startActivity(Intent(requireContext(), AddEditTimetableActivity::class.java))
        }
    }

    private fun setUpDayChips() {
        binding.chipGroupDay.removeAllViews()
        daysOfWeek.forEach { day ->
            val chip = Chip(requireContext()).apply {
                text = day
                isCheckable = true
                isChecked = day == selectedDay
                setOnClickListener {
                    selectedDay = day
                    applyDayFilter()
                }
            }
            binding.chipGroupDay.addView(chip)
        }
    }

    override fun onResume() {
        super.onResume()
        loadModules()
        loadTimetable()
    }

    private fun loadModules() {
        lifecycleScope.launch {
            when (val result = moduleRepository.getModules()) {
                is ApiResult.Success -> adapter.setModules(result.data)
                is ApiResult.Failure -> Log.w(tag, "Could not load modules for timetable: ${result.message}")
            }
        }
    }

    private fun loadTimetable() {
        lifecycleScope.launch {
            when (val result = timetableRepository.getTimetable()) {
                is ApiResult.Success -> {
                    Log.d(tag, "Loaded ${result.data.size} timetable entries")
                    allEntries = result.data
                    applyDayFilter()
                }
                is ApiResult.Failure -> {
                    Log.e(tag, "Failed to load timetable: ${result.message}")
                    Toast.makeText(requireContext(), result.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun applyDayFilter() {
        val filtered = allEntries
            .filter { it.dayOfWeek == selectedDay }
            .sortedBy { it.startTime }
        adapter.submitList(filtered)
        binding.textNoClasses.visibility = if (filtered.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun openEditor(entry: TimetableEntry) {
        val intent = Intent(requireContext(), AddEditTimetableActivity::class.java).apply {
            putExtra(AddEditTimetableActivity.EXTRA_ENTRY_ID, entry.id)
            putExtra(AddEditTimetableActivity.EXTRA_ENTRY_MODULE_ID, entry.moduleId)
            putExtra(AddEditTimetableActivity.EXTRA_ENTRY_DAY, entry.dayOfWeek)
            putExtra(AddEditTimetableActivity.EXTRA_ENTRY_START, entry.startTime)
            putExtra(AddEditTimetableActivity.EXTRA_ENTRY_END, entry.endTime)
            putExtra(AddEditTimetableActivity.EXTRA_ENTRY_VENUE, entry.venue)
        }
        startActivity(intent)
    }

    private fun confirmDelete(entry: TimetableEntry) {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.delete)
            .setMessage(R.string.delete)
            .setPositiveButton(R.string.delete) { _, _ -> deleteEntry(entry) }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun deleteEntry(entry: TimetableEntry) {
        lifecycleScope.launch {
            when (val result = timetableRepository.deleteEntry(entry.id)) {
                is ApiResult.Success -> loadTimetable()
                is ApiResult.Failure -> Toast.makeText(requireContext(), result.message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun currentDayOfWeek(): String {
            val days = listOf("Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday")
            val index = Calendar.getInstance().get(Calendar.DAY_OF_WEEK) - 1
            return days[index]
        }
    }
}

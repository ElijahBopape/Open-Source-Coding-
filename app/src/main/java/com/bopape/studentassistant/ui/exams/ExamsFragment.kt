package com.bopape.studentassistant.ui.exams

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
import com.bopape.studentassistant.data.repository.ExamRepository
import com.bopape.studentassistant.data.repository.ModuleRepository
import com.bopape.studentassistant.databinding.FragmentExamsBinding
import com.bopape.studentassistant.util.ApiResult
import com.bopape.studentassistant.util.DateUtils
import com.bopape.studentassistant.util.StudyPlanner
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * Examination management (User Defined feature 4, Part 1 design section 3.6).
 * Shows upcoming exams with a countdown, or past exams with the mark obtained,
 * plus the student's average mark (academic progress, section 3.7).
 */
class ExamsFragment : Fragment() {

    private var _binding: FragmentExamsBinding? = null
    private val binding get() = _binding!!

    private val examRepository = ExamRepository()
    private val moduleRepository = ModuleRepository()
    private lateinit var adapter: ExamAdapter
    private var allExams: List<Exam> = emptyList()
    private var showPast = false
    private val tag = "ExamsFragment"

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentExamsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = ExamAdapter(onClick = { openEditor(it) }, onDelete = { confirmDelete(it) })
        binding.recyclerExams.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerExams.adapter = adapter

        binding.chipGroupExamFilter.setOnCheckedStateChangeListener { _, ids ->
            showPast = ids.firstOrNull() == R.id.chipPast
            applyFilter()
        }
        binding.fabAddExam.setOnClickListener {
            startActivity(Intent(requireContext(), AddEditExamActivity::class.java))
        }
        binding.swipeRefresh.setOnRefreshListener { loadExams() }
    }

    override fun onResume() {
        super.onResume()
        loadExams()
    }

    private fun loadExams() {
        viewLifecycleOwner.lifecycleScope.launch {
            (moduleRepository.getModules() as? ApiResult.Success)?.let { adapter.setModules(it.data) }
            when (val result = examRepository.getExams()) {
                is ApiResult.Success -> {
                    Log.d(tag, "Loaded ${result.data.size} exams (fromCache=${result.fromCache})")
                    allExams = result.data
                    applyFilter()
                }
                is ApiResult.Failure -> {
                    Log.e(tag, "Failed to load exams: ${result.message}")
                    Toast.makeText(requireContext(), result.message, Toast.LENGTH_SHORT).show()
                }
            }
            _binding?.swipeRefresh?.isRefreshing = false
        }
    }

    private fun applyFilter() {
        if (_binding == null) return
        val filtered = allExams.filter {
            val days = DateUtils.daysUntil(it.date) ?: 0
            if (showPast) days < 0 else days >= 0
        }.let { list -> if (showPast) list.sortedByDescending { it.date } else list.sortedBy { it.date + it.time } }

        adapter.submitList(filtered)
        binding.textNoExams.setText(if (showPast) R.string.no_past_exams else R.string.no_exams_message)
        binding.textNoExams.visibility = if (filtered.isEmpty()) View.VISIBLE else View.GONE

        val average = StudyPlanner.averageMark(allExams)
        binding.textAverage.text = if (average == null) {
            getString(R.string.exams_average_none)
        } else {
            getString(R.string.exams_average, average.roundToInt(), allExams.count { it.mark != null })
        }
    }

    private fun openEditor(exam: Exam) {
        startActivity(AddEditExamActivity.editIntent(requireContext(), exam))
    }

    private fun confirmDelete(exam: Exam) {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.delete_exam_confirm_title)
            .setMessage(getString(R.string.delete_exam_confirm_message, exam.title))
            .setPositiveButton(R.string.delete) { _, _ ->
                viewLifecycleOwner.lifecycleScope.launch {
                    when (val result = examRepository.deleteExam(exam.id)) {
                        is ApiResult.Success -> loadExams()
                        is ApiResult.Failure -> Toast.makeText(requireContext(), result.message, Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

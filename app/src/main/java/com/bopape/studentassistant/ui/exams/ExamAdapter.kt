package com.bopape.studentassistant.ui.exams

import android.annotation.SuppressLint
import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bopape.studentassistant.R
import com.bopape.studentassistant.data.model.Exam
import com.bopape.studentassistant.data.model.Module
import com.bopape.studentassistant.databinding.ItemExamBinding
import com.bopape.studentassistant.util.DateUtils
import com.bopape.studentassistant.util.displayDate
import com.bopape.studentassistant.util.relativeDays
import kotlin.math.roundToInt

/** Exam rows: date and countdown badge for upcoming exams, the mark for past ones. */
class ExamAdapter(
    private val onClick: (Exam) -> Unit,
    private val onDelete: (Exam) -> Unit
) : RecyclerView.Adapter<ExamAdapter.ExamViewHolder>() {

    private val exams = mutableListOf<Exam>()
    private var modulesById: Map<Int, Module> = emptyMap()

    @SuppressLint("NotifyDataSetChanged")
    fun submitList(newExams: List<Exam>) {
        exams.clear()
        exams.addAll(newExams)
        notifyDataSetChanged()
    }

    @SuppressLint("NotifyDataSetChanged")
    fun setModules(modules: List<Module>) {
        modulesById = modules.associateBy { it.id }
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ExamViewHolder(ItemExamBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: ExamViewHolder, position: Int) = holder.bind(exams[position])

    override fun getItemCount(): Int = exams.size

    inner class ExamViewHolder(private val binding: ItemExamBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(exam: Exam) {
            val context = binding.root.context
            binding.textExamTitle.text = exam.title
            val module = exam.moduleId?.let { modulesById[it] }
            binding.textExamModule.text = module?.let { if (it.code.isBlank()) it.name else "${it.code} · ${it.name}" }
                ?: context.getString(R.string.no_module)
            binding.textExamWhen.text = listOf(
                context.displayDate(exam.date),
                exam.time,
                exam.venue
            ).filter { it.isNotBlank() }.joinToString(" · ")

            val days = DateUtils.daysUntil(exam.date) ?: 0
            val (badgeText, colorRes) = when {
                days < 0 && exam.mark != null -> context.getString(R.string.percent_value, exam.mark.roundToInt()) to
                    if (exam.mark >= 50) R.color.priority_low else R.color.priority_high
                days < 0 -> context.getString(R.string.exam_written) to R.color.on_surface_muted
                days <= 3 -> context.relativeDays(exam.date) to R.color.priority_high
                days <= 7 -> context.relativeDays(exam.date) to R.color.priority_medium
                else -> context.relativeDays(exam.date) to R.color.primary
            }
            binding.textExamBadge.text = badgeText
            binding.textExamBadge.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(context, colorRes))

            binding.root.setOnClickListener { onClick(exam) }
            binding.buttonDeleteExam.setOnClickListener { onDelete(exam) }
        }
    }
}

package com.bopape.studentassistant.ui.study

import android.annotation.SuppressLint
import android.graphics.Paint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bopape.studentassistant.R
import com.bopape.studentassistant.data.model.Exam
import com.bopape.studentassistant.data.model.StudySession
import com.bopape.studentassistant.data.model.Task
import com.bopape.studentassistant.databinding.ItemStudySessionBinding
import com.bopape.studentassistant.util.displayDate
import com.bopape.studentassistant.util.relativeDays

/** Study session rows: date, time, duration, and what the session is preparing for. */
class StudySessionAdapter(
    private val onClick: (StudySession) -> Unit,
    private val onToggle: (StudySession, Boolean) -> Unit,
    private val onDelete: (StudySession) -> Unit
) : RecyclerView.Adapter<StudySessionAdapter.SessionViewHolder>() {

    private val sessions = mutableListOf<StudySession>()
    private var examsById: Map<Int, Exam> = emptyMap()
    private var tasksByRemoteId: Map<Int, Task> = emptyMap()

    @SuppressLint("NotifyDataSetChanged")
    fun submitList(items: List<StudySession>) {
        sessions.clear()
        sessions.addAll(items)
        notifyDataSetChanged()
    }

    @SuppressLint("NotifyDataSetChanged")
    fun setLinks(exams: List<Exam>, tasks: List<Task>) {
        examsById = exams.associateBy { it.id }
        tasksByRemoteId = tasks.filter { it.remoteId != null }.associateBy { it.remoteId!! }
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        SessionViewHolder(ItemStudySessionBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: SessionViewHolder, position: Int) = holder.bind(sessions[position])

    override fun getItemCount() = sessions.size

    inner class SessionViewHolder(private val binding: ItemStudySessionBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(session: StudySession) {
            val context = binding.root.context
            binding.textSessionTitle.text = session.title
            binding.textSessionTitle.paintFlags = if (session.completed) {
                binding.textSessionTitle.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
            } else {
                binding.textSessionTitle.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
            }

            binding.textSessionWhen.text = listOf(
                context.displayDate(session.date),
                context.relativeDays(session.date),
                session.startTime,
                context.getString(R.string.minutes_short, session.durationMinutes)
            ).filter { it.isNotBlank() }.joinToString(" · ")

            val link = session.examId?.let { examsById[it]?.title?.let { t -> context.getString(R.string.linked_exam, t) } }
                ?: session.taskId?.let { tasksByRemoteId[it]?.title?.let { t -> context.getString(R.string.linked_task, t) } }
            binding.textSessionLink.text = link.orEmpty()
            binding.textSessionLink.visibility = if (link == null) android.view.View.GONE else android.view.View.VISIBLE

            binding.checkboxSessionDone.setOnCheckedChangeListener(null)
            binding.checkboxSessionDone.isChecked = session.completed
            binding.checkboxSessionDone.setOnCheckedChangeListener { _, checked -> onToggle(session, checked) }

            binding.root.setOnClickListener { onClick(session) }
            binding.buttonDeleteSession.setOnClickListener { onDelete(session) }
        }
    }
}

package com.bopape.studentassistant.ui.timetable

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bopape.studentassistant.data.model.Module
import com.bopape.studentassistant.data.model.TimetableEntry
import com.bopape.studentassistant.databinding.ItemTimetableBinding

class TimetableAdapter(
    private val onClick: (TimetableEntry) -> Unit,
    private val onDelete: (TimetableEntry) -> Unit
) : RecyclerView.Adapter<TimetableAdapter.TimetableViewHolder>() {

    private val entries = mutableListOf<TimetableEntry>()
    private var modulesById: Map<Int, Module> = emptyMap()

    fun submitList(newEntries: List<TimetableEntry>) {
        entries.clear()
        entries.addAll(newEntries)
        notifyDataSetChanged()
    }

    fun setModules(modules: List<Module>) {
        modulesById = modules.associateBy { it.id }
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TimetableViewHolder {
        val binding = ItemTimetableBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return TimetableViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TimetableViewHolder, position: Int) {
        holder.bind(entries[position])
    }

    override fun getItemCount(): Int = entries.size

    inner class TimetableViewHolder(private val binding: ItemTimetableBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(entry: TimetableEntry) {
            binding.textStartTime.text = entry.startTime
            binding.textEndTime.text = entry.endTime

            val module = modulesById[entry.moduleId]
            binding.textClassModule.text = module?.name ?: "Unknown module"
            binding.textClassVenue.text = listOfNotNull(
                entry.venue.ifBlank { null },
                module?.lecturer?.ifBlank { null }
            ).joinToString(" · ")

            binding.root.setOnClickListener { onClick(entry) }
            binding.buttonDeleteClass.setOnClickListener { onDelete(entry) }
        }
    }
}

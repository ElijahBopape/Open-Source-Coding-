package com.bopape.studentassistant.ui.modules

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bopape.studentassistant.data.model.Module
import com.bopape.studentassistant.databinding.ItemModuleBinding

class ModuleAdapter(
    private val onClick: (Module) -> Unit,
    private val onDelete: (Module) -> Unit
) : RecyclerView.Adapter<ModuleAdapter.ModuleViewHolder>() {

    private val modules = mutableListOf<Module>()

    fun submitList(newModules: List<Module>) {
        modules.clear()
        modules.addAll(newModules)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ModuleViewHolder {
        val binding = ItemModuleBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ModuleViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ModuleViewHolder, position: Int) {
        holder.bind(modules[position])
    }

    override fun getItemCount(): Int = modules.size

    inner class ModuleViewHolder(private val binding: ItemModuleBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(module: Module) {
            binding.textModuleName.text = module.name
            binding.textModuleDetail.text = listOfNotNull(
                module.code.ifBlank { null },
                module.lecturer.ifBlank { null },
                module.venue.ifBlank { null }
            ).joinToString(" · ")

            binding.root.setOnClickListener { onClick(module) }
            binding.buttonDeleteModule.setOnClickListener { onDelete(module) }
        }
    }
}

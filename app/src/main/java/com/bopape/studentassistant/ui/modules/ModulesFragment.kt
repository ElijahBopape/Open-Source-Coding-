package com.bopape.studentassistant.ui.modules

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
import com.bopape.studentassistant.data.repository.ModuleRepository
import com.bopape.studentassistant.databinding.FragmentModulesBinding
import com.bopape.studentassistant.util.ApiResult
import kotlinx.coroutines.launch

/** Lists the user's academic modules and lets them add, edit or delete one. */
class ModulesFragment : Fragment() {

    private var _binding: FragmentModulesBinding? = null
    private val binding get() = _binding!!

    private val repository = ModuleRepository()
    private lateinit var adapter: ModuleAdapter
    private val tag = "ModulesFragment"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentModulesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = ModuleAdapter(
            onClick = { openEditor(it) },
            onDelete = { confirmDelete(it) }
        )
        binding.recyclerModules.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerModules.adapter = adapter

        binding.fabAddModule.setOnClickListener {
            startActivity(Intent(requireContext(), AddEditModuleActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        loadModules()
    }

    private fun loadModules() {
        lifecycleScope.launch {
            when (val result = repository.getModules()) {
                is ApiResult.Success -> {
                    Log.d(tag, "Loaded ${result.data.size} modules")
                    adapter.submitList(result.data)
                    binding.textNoModules.visibility = if (result.data.isEmpty()) View.VISIBLE else View.GONE
                }
                is ApiResult.Failure -> {
                    Log.e(tag, "Failed to load modules: ${result.message}")
                    Toast.makeText(requireContext(), result.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun openEditor(module: Module) {
        val intent = Intent(requireContext(), AddEditModuleActivity::class.java).apply {
            putExtra(AddEditModuleActivity.EXTRA_MODULE_ID, module.id)
            putExtra(AddEditModuleActivity.EXTRA_MODULE_NAME, module.name)
            putExtra(AddEditModuleActivity.EXTRA_MODULE_CODE, module.code)
            putExtra(AddEditModuleActivity.EXTRA_MODULE_LECTURER, module.lecturer)
            putExtra(AddEditModuleActivity.EXTRA_MODULE_VENUE, module.venue)
        }
        startActivity(intent)
    }

    private fun confirmDelete(module: Module) {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.delete_module_confirm_title)
            .setMessage(R.string.delete_module_confirm_message)
            .setPositiveButton(R.string.delete) { _, _ -> deleteModule(module) }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun deleteModule(module: Module) {
        lifecycleScope.launch {
            when (val result = repository.deleteModule(module.id)) {
                is ApiResult.Success -> loadModules()
                is ApiResult.Failure -> Toast.makeText(requireContext(), result.message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

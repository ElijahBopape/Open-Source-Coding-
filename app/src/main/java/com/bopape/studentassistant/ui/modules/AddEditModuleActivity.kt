package com.bopape.studentassistant.ui.modules

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.bopape.studentassistant.R
import com.bopape.studentassistant.data.model.Module
import com.bopape.studentassistant.data.repository.ModuleRepository
import com.bopape.studentassistant.databinding.ActivityAddEditModuleBinding
import com.bopape.studentassistant.util.ApiResult
import kotlinx.coroutines.launch

/** Creates a new module, or edits an existing one when launched with EXTRA_MODULE_ID. */
class AddEditModuleActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddEditModuleBinding
    private val repository = ModuleRepository()
    private var editingModuleId: Int = 0
    private val tag = "AddEditModuleActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddEditModuleBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toolbar.setNavigationOnClickListener { finish() }

        editingModuleId = intent.getIntExtra(EXTRA_MODULE_ID, 0)
        if (editingModuleId != 0) {
            binding.toolbar.title = getString(R.string.edit_module_title)
            binding.editModuleName.setText(intent.getStringExtra(EXTRA_MODULE_NAME))
            binding.editModuleCode.setText(intent.getStringExtra(EXTRA_MODULE_CODE))
            binding.editLecturer.setText(intent.getStringExtra(EXTRA_MODULE_LECTURER))
            binding.editVenue.setText(intent.getStringExtra(EXTRA_MODULE_VENUE))
        }

        binding.buttonSaveModule.setOnClickListener { saveModule() }
    }

    private fun saveModule() {
        val name = binding.editModuleName.text.toString().trim()
        if (name.isEmpty()) {
            binding.editModuleName.error = getString(R.string.error_name_required)
            return
        }

        val module = Module(
            id = editingModuleId,
            name = name,
            code = binding.editModuleCode.text.toString().trim(),
            lecturer = binding.editLecturer.text.toString().trim(),
            venue = binding.editVenue.text.toString().trim()
        )

        setLoading(true)
        lifecycleScope.launch {
            val result = if (editingModuleId == 0) {
                Log.d(tag, "Creating module '$name'")
                repository.createModule(module)
            } else {
                Log.d(tag, "Updating module #$editingModuleId")
                repository.updateModule(module)
            }
            setLoading(false)
            when (result) {
                is ApiResult.Success -> finish()
                is ApiResult.Failure -> Toast.makeText(this@AddEditModuleActivity, result.message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setLoading(loading: Boolean) {
        binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        binding.buttonSaveModule.isEnabled = !loading
    }

    companion object {
        const val EXTRA_MODULE_ID = "extra_module_id"
        const val EXTRA_MODULE_NAME = "extra_module_name"
        const val EXTRA_MODULE_CODE = "extra_module_code"
        const val EXTRA_MODULE_LECTURER = "extra_module_lecturer"
        const val EXTRA_MODULE_VENUE = "extra_module_venue"
    }
}

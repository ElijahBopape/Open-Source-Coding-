package com.bopape.studentassistant.ui.settings

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import com.bopape.studentassistant.R
import com.bopape.studentassistant.databinding.FragmentSettingsBinding
import com.bopape.studentassistant.ui.auth.LoginActivity
import com.bopape.studentassistant.util.SessionManager
import com.google.firebase.auth.FirebaseAuth

/**
 * Lets the user update their local preferences (language, notifications, theme) and
 * sign out. Preferences persist across app restarts via [SessionManager].
 */
class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private lateinit var sessionManager: SessionManager
    private val tag = "SettingsFragment"
    private var isRestoringState = true

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        sessionManager = SessionManager(requireContext())

        bindAccountInfo()
        bindPreferences()

        binding.buttonSignOut.setOnClickListener { signOut() }
    }

    private fun bindAccountInfo() {
        val user = FirebaseAuth.getInstance().currentUser
        val name = user?.displayName?.ifBlank { null } ?: "Student"
        binding.textUserName.text = name
        binding.textUserEmail.text = user?.email ?: ""
        binding.textAvatarInitial.text = name.first().uppercase()
    }

    private fun bindPreferences() {
        val languages = resources.getStringArray(R.array.language_options)
        binding.spinnerLanguage.adapter = ArrayAdapter(
            requireContext(), android.R.layout.simple_spinner_dropdown_item, languages
        )
        val languageIndex = languages.indexOf(sessionManager.language).coerceAtLeast(0)
        binding.spinnerLanguage.setSelection(languageIndex)

        binding.switchNotifications.isChecked = sessionManager.notificationsEnabled
        binding.switchDarkTheme.isChecked = sessionManager.darkThemeEnabled

        // Listeners are attached after the initial values are set so restoring
        // saved preferences does not itself trigger a "settings saved" toast.
        isRestoringState = false

        binding.spinnerLanguage.post {
            binding.spinnerLanguage.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: android.widget.AdapterView<*>?, v: View?, position: Int, id: Long) {
                    if (isRestoringState) return
                    sessionManager.language = languages[position]
                    Log.d(tag, "Language preference set to ${languages[position]}")
                    showSaved()
                }
                override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
            }
        }

        binding.switchNotifications.setOnCheckedChangeListener { _, isChecked ->
            sessionManager.notificationsEnabled = isChecked
            Log.d(tag, "Notifications preference set to $isChecked")
            showSaved()
        }

        binding.switchDarkTheme.setOnCheckedChangeListener { _, isChecked ->
            sessionManager.darkThemeEnabled = isChecked
            AppCompatDelegate.setDefaultNightMode(
                if (isChecked) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
            )
            Log.d(tag, "Dark theme preference set to $isChecked")
            showSaved()
        }
    }

    private fun showSaved() {
        Toast.makeText(requireContext(), R.string.settings_saved_message, Toast.LENGTH_SHORT).show()
    }

    private fun signOut() {
        Log.i(tag, "Signing out ${FirebaseAuth.getInstance().currentUser?.uid}")
        FirebaseAuth.getInstance().signOut()
        val intent = Intent(requireContext(), LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

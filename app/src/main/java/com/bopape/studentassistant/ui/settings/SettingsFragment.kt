package com.bopape.studentassistant.ui.settings

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.format.DateUtils as AndroidDateUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import coil.load
import coil.transform.CircleCropTransformation
import com.bopape.studentassistant.AppGraph
import com.bopape.studentassistant.BuildConfig
import com.bopape.studentassistant.R
import com.bopape.studentassistant.data.model.UserProfile
import com.bopape.studentassistant.data.repository.ProfileRepository
import com.bopape.studentassistant.data.repository.TaskRepository
import com.bopape.studentassistant.data.sync.SyncBus
import com.bopape.studentassistant.data.sync.SyncScheduler
import com.bopape.studentassistant.databinding.FragmentSettingsBinding
import com.bopape.studentassistant.notifications.PushRegistrar
import com.bopape.studentassistant.ui.auth.GoogleSignInHelper
import com.bopape.studentassistant.ui.auth.LoginActivity
import com.bopape.studentassistant.util.ApiResult
import com.bopape.studentassistant.util.LocaleHelper
import com.bopape.studentassistant.util.SessionManager
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * Settings the user can change (Part 1 design section 3.2):
 *  - profile: display name and profile photo (photo stored in Cloudinary blob storage);
 *  - language: English, Sepedi or isiZulu, applied instantly;
 *  - push notifications on/off (also updates the server so it stops sending);
 *  - light/dark theme;
 *  - offline sync status with a "Sync now" button;
 *  - a "Send test notification" button to prove the FCM push path end-to-end;
 *  - sign out.
 * Every preference is saved on the device (SessionManager) and in the user's
 * Firestore profile through the REST API, so it follows them between devices.
 */
class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private lateinit var session: SessionManager
    private val profileRepository = ProfileRepository()
    private val taskRepository = TaskRepository()
    private val tag = "SettingsFragment"
    private var bindingPreferences = false

    /** Android photo picker - needs no storage permission. */
    private val pickPhoto = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) uploadPhoto(uri)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        session = SessionManager(requireContext())

        bindAccountInfo()
        bindPreferences()
        observeSyncState()

        binding.buttonChangePhoto.setOnClickListener {
            pickPhoto.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }
        binding.buttonEditName.setOnClickListener { showEditNameDialog() }
        binding.buttonSyncNow.setOnClickListener { syncNow() }
        binding.buttonTestNotification.setOnClickListener { sendTestNotification() }
        binding.buttonSignOut.setOnClickListener { confirmSignOut() }
        binding.textAppVersion.text = getString(R.string.app_version, BuildConfig.VERSION_NAME)
    }

    // ---- Profile ---------------------------------------------------------------

    private fun bindAccountInfo() {
        val user = FirebaseAuth.getInstance().currentUser
        val name = user?.displayName?.ifBlank { null } ?: getString(R.string.default_student_name)
        binding.textUserName.text = name
        binding.textUserEmail.text = user?.email.orEmpty()
        binding.textAvatarInitial.text = name.first().uppercase()

        val provider = user?.providerData?.map { it.providerId }.orEmpty()
        binding.textSignInMethod.text = getString(
            if ("google.com" in provider) R.string.signed_in_with_google else R.string.signed_in_with_email
        )
        showPhoto(user?.photoUrl?.toString())
    }

    private fun showPhoto(url: String?) {
        if (url.isNullOrBlank()) {
            binding.imageAvatar.visibility = View.GONE
            binding.textAvatarInitial.visibility = View.VISIBLE
            return
        }
        binding.imageAvatar.visibility = View.VISIBLE
        binding.textAvatarInitial.visibility = View.INVISIBLE
        binding.imageAvatar.load(url) {
            crossfade(true)
            transformations(CircleCropTransformation())
            listener(onError = { _, _ ->
                _binding?.imageAvatar?.visibility = View.GONE
                _binding?.textAvatarInitial?.visibility = View.VISIBLE
            })
        }
    }

    /** Uploads the photo to Cloudinary, then saves its URL in Firebase Auth and Firestore. */
    private fun uploadPhoto(uri: Uri) {
        binding.progressPhoto.visibility = View.VISIBLE
        viewLifecycleOwner.lifecycleScope.launch {
            when (val upload = profileRepository.uploadProfilePhoto(requireContext(), uri)) {
                is ApiResult.Success -> {
                    val url = upload.data
                    try {
                        FirebaseAuth.getInstance().currentUser
                            ?.updateProfile(UserProfileChangeRequest.Builder().setPhotoUri(Uri.parse(url)).build())
                            ?.await()
                    } catch (e: Exception) {
                        Log.w(tag, "Could not save photo URL on the Firebase user", e)
                    }
                    profileRepository.saveProfile(UserProfile(photoUrl = url))
                    if (_binding != null) {
                        showPhoto(url)
                        Toast.makeText(requireContext(), R.string.photo_updated, Toast.LENGTH_SHORT).show()
                    }
                }
                is ApiResult.Failure -> Toast.makeText(requireContext(), upload.message, Toast.LENGTH_LONG).show()
            }
            _binding?.progressPhoto?.visibility = View.GONE
        }
    }

    private fun showEditNameDialog() {
        val input = TextInputEditText(requireContext()).apply {
            setText(FirebaseAuth.getInstance().currentUser?.displayName)
            hint = getString(R.string.full_name_hint)
            setSingleLine()
        }
        val pad = resources.getDimensionPixelSize(R.dimen.dialog_padding)
        val container = FrameLayout(requireContext()).apply {
            setPadding(pad, pad / 2, pad, 0)
            addView(input)
        }
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.edit_name_title)
            .setView(container)
            .setPositiveButton(R.string.save) { _, _ ->
                val name = input.text.toString().trim()
                if (name.isEmpty() || name.length > 80) {
                    Toast.makeText(requireContext(), R.string.error_name_required, Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                saveName(name)
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun saveName(name: String) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                FirebaseAuth.getInstance().currentUser
                    ?.updateProfile(UserProfileChangeRequest.Builder().setDisplayName(name).build())?.await()
                profileRepository.saveProfile(UserProfile(displayName = name))
                Log.i(tag, "Display name updated")
                if (_binding != null) {
                    bindAccountInfo()
                    showSaved()
                }
            } catch (e: Exception) {
                Log.w(tag, "Could not update display name", e)
                Toast.makeText(requireContext(), R.string.error_network, Toast.LENGTH_SHORT).show()
            }
        }
    }

    // ---- Preferences -----------------------------------------------------------

    private fun bindPreferences() {
        bindingPreferences = true
        val languages = resources.getStringArray(R.array.language_options)
        binding.spinnerLanguage.adapter =
            ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, languages)
        binding.spinnerLanguage.setSelection(LocaleHelper.supportedCodes.indexOf(LocaleHelper.currentCode()).coerceAtLeast(0), false)

        binding.switchNotifications.isChecked = session.notificationsEnabled
        binding.switchDarkTheme.isChecked = session.darkThemeEnabled
        bindingPreferences = false

        binding.spinnerLanguage.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, v: View?, position: Int, id: Long) {
                if (bindingPreferences) return
                val code = LocaleHelper.supportedCodes[position]
                if (code == LocaleHelper.currentCode()) return
                session.language = code
                Log.d(tag, "Language set to $code")
                saveToProfile(UserProfile(language = code))
                // Recreates every open screen in the new language.
                LocaleHelper.apply(code)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
        }

        binding.switchNotifications.setOnCheckedChangeListener { _, isChecked ->
            session.notificationsEnabled = isChecked
            Log.d(tag, "Notifications set to $isChecked")
            saveToProfile(UserProfile(notificationsEnabled = isChecked))
            if (isChecked) PushRegistrar.registerCurrentDevice(requireContext())
            binding.buttonTestNotification.isEnabled = isChecked
            showSaved()
        }
        binding.buttonTestNotification.isEnabled = session.notificationsEnabled

        binding.switchDarkTheme.setOnCheckedChangeListener { _, isChecked ->
            session.darkThemeEnabled = isChecked
            Log.d(tag, "Dark theme set to $isChecked")
            saveToProfile(UserProfile(darkTheme = isChecked))
            AppCompatDelegate.setDefaultNightMode(
                if (isChecked) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
            )
        }
    }

    /** Saves a settings change to Firestore in the background; it is already saved locally. */
    private fun saveToProfile(change: UserProfile) {
        lifecycleScope.launch {
            when (val result = profileRepository.saveProfile(change)) {
                is ApiResult.Success -> Log.d(tag, "Settings saved to Firestore profile")
                is ApiResult.Failure -> Log.w(tag, "Settings saved on device only: ${result.message}")
            }
        }
    }

    // ---- Sync & notifications ------------------------------------------------

    private fun observeSyncState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { taskRepository.observePendingCount().collect { updateSyncText(it) } }
                launch { SyncBus.events.collect { updateSyncText(taskRepository.pendingCount()) } }
            }
        }
    }

    private fun updateSyncText(pending: Int) {
        val b = _binding ?: return
        val last = session.lastSyncMillis
        val lastText = if (last == 0L) getString(R.string.sync_never) else
            AndroidDateUtils.getRelativeTimeSpanString(last, System.currentTimeMillis(), AndroidDateUtils.MINUTE_IN_MILLIS).toString()
        b.textSyncStatus.text = getString(R.string.sync_last, lastText)
        b.textSyncPending.text = if (pending == 0) getString(R.string.sync_all_done)
        else resources.getQuantityString(R.plurals.sync_pending, pending, pending)
    }

    private fun syncNow() {
        binding.buttonSyncNow.isEnabled = false
        viewLifecycleOwner.lifecycleScope.launch {
            val outcome = taskRepository.syncNow()
            SyncScheduler.requestSync(requireContext())
            val b = _binding ?: return@launch
            b.buttonSyncNow.isEnabled = true
            Toast.makeText(
                requireContext(),
                if (outcome.success) getString(R.string.sync_done, outcome.pushed) else getString(R.string.sync_offline_message),
                Toast.LENGTH_SHORT
            ).show()
            updateSyncText(taskRepository.pendingCount())
        }
    }

    /** Asks the REST API to push a notification to this device through FCM. */
    private fun sendTestNotification() {
        PushRegistrar.registerCurrentDevice(requireContext())
        viewLifecycleOwner.lifecycleScope.launch {
            val message = when (val result = profileRepository.sendTestNotification()) {
                is ApiResult.Success -> if (result.data.sent > 0) getString(R.string.test_notification_sent)
                else getString(R.string.test_notification_no_device)
                is ApiResult.Failure -> result.message
            }
            Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
        }
    }

    // ---- Sign out --------------------------------------------------------------

    private fun confirmSignOut() {
        viewLifecycleOwner.lifecycleScope.launch {
            val pending = taskRepository.pendingCount()
            if (pending == 0) {
                signOut()
                return@launch
            }
            // Signing out clears this device's offline data, so warn about unsynced changes.
            AlertDialog.Builder(requireContext())
                .setTitle(R.string.sign_out_pending_title)
                .setMessage(resources.getQuantityString(R.plurals.sign_out_pending_message, pending, pending))
                .setPositiveButton(R.string.sign_out_button) { _, _ -> signOut() }
                .setNegativeButton(R.string.cancel, null)
                .show()
        }
    }

    private fun signOut() {
        val context = requireContext().applicationContext
        val uid = AppGraph.currentUserId
        Log.i(tag, "Signing out $uid")
        lifecycleScope.launch {
            PushRegistrar.unregister(context)
            GoogleSignInHelper(context).clear()
            SyncScheduler.cancelAll(context)
            if (uid != null) {
                AppGraph.database.taskDao().clearUser(uid)
                AppGraph.database.cacheDao().clearUser(uid)
            }
            session.clearAccountData()
            FirebaseAuth.getInstance().signOut()
            startActivity(Intent(context, LoginActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            })
        }
    }

    private fun showSaved() {
        Toast.makeText(requireContext(), R.string.settings_saved_message, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

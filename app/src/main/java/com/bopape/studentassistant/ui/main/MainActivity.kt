package com.bopape.studentassistant.ui.main

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.bopape.studentassistant.AppGraph
import com.bopape.studentassistant.R
import com.bopape.studentassistant.data.repository.TaskRepository
import com.bopape.studentassistant.databinding.ActivityMainBinding
import com.bopape.studentassistant.notifications.NotificationHelper
import com.bopape.studentassistant.ui.auth.LoginActivity
import com.bopape.studentassistant.ui.dashboard.DashboardFragment
import com.bopape.studentassistant.ui.exams.ExamsFragment
import com.bopape.studentassistant.ui.settings.SettingsFragment
import com.bopape.studentassistant.ui.tasks.TasksFragment
import com.bopape.studentassistant.ui.timetable.TimetableFragment
import com.bopape.studentassistant.util.applySystemBarInsets
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/**
 * Hosts the bottom navigation (Dashboard, Tasks, Timetable, Exams, Settings) and
 * swaps between the main fragments. Modules and the Study Planner open from the
 * dashboard's quick actions. A single Activity + Fragment structure keeps
 * navigation simple, as recommended by the Android Developers navigation guide.
 *
 * Also shows the offline banner and asks for the notification permission on
 * Android 13+, which push notifications need.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val tag = "MainActivity"

    private val notificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            Log.i(tag, "POST_NOTIFICATIONS permission granted=$granted")
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.root.applySystemBarInsets()

        if (FirebaseAuth.getInstance().currentUser == null) {
            Log.w(tag, "No signed-in user, returning to login")
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        binding.bottomNavigation.setOnItemSelectedListener { item ->
            val fragment: Fragment = when (item.itemId) {
                R.id.nav_tasks -> TasksFragment()
                R.id.nav_timetable -> TimetableFragment()
                R.id.nav_exams -> ExamsFragment()
                R.id.nav_settings -> SettingsFragment()
                else -> DashboardFragment()
            }
            Log.d(tag, "Switching to ${fragment.javaClass.simpleName}")
            showFragment(fragment)
            true
        }

        if (savedInstanceState == null) {
            if (!openTabFromIntent(intent)) showFragment(DashboardFragment())
        }

        observeConnectivity()
        askForNotificationPermission()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        openTabFromIntent(intent)
    }

    /** Lets the dashboard switch tabs (e.g. "See all tasks"). */
    fun selectTab(itemId: Int) {
        binding.bottomNavigation.selectedItemId = itemId
    }

    /** Tapping a task/exam notification opens the matching tab. */
    private fun openTabFromIntent(intent: Intent?): Boolean {
        val tab = when (intent?.getStringExtra(NotificationHelper.EXTRA_OPEN_TAB)) {
            "task" -> R.id.nav_tasks
            "exam" -> R.id.nav_exams
            else -> return false
        }
        binding.bottomNavigation.selectedItemId = tab
        return true
    }

    /** Shows "Offline - N change(s) will sync" while there is no connection. */
    private fun observeConnectivity() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                AppGraph.networkMonitor.isOnline
                    .combine(TaskRepository().observePendingCount()) { online, pending -> online to pending }
                    .collect { (online, pending) ->
                        binding.textOfflineBanner.visibility = if (online) View.GONE else View.VISIBLE
                        binding.textOfflineBanner.text = if (pending > 0) {
                            resources.getQuantityString(R.plurals.offline_banner_pending, pending, pending)
                        } else {
                            getString(R.string.offline_banner)
                        }
                    }
            }
        }
    }

    private fun askForNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun showFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }
}

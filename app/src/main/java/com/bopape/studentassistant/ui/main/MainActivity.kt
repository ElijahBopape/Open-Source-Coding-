package com.bopape.studentassistant.ui.main

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.bopape.studentassistant.R
import com.bopape.studentassistant.databinding.ActivityMainBinding
import com.bopape.studentassistant.ui.auth.LoginActivity
import com.bopape.studentassistant.ui.dashboard.DashboardFragment
import com.bopape.studentassistant.ui.modules.ModulesFragment
import com.bopape.studentassistant.ui.settings.SettingsFragment
import com.bopape.studentassistant.ui.tasks.TasksFragment
import com.bopape.studentassistant.ui.timetable.TimetableFragment
import com.google.firebase.auth.FirebaseAuth

/**
 * Hosts the bottom navigation and swaps between the five main fragments.
 * A single Activity + Fragment structure keeps navigation simple, as recommended
 * by the Android Developers navigation guide (Android Developers, 2026).
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val tag = "MainActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (FirebaseAuth.getInstance().currentUser == null) {
            Log.w(tag, "No signed-in user, returning to login")
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        if (savedInstanceState == null) {
            showFragment(DashboardFragment())
        }

        binding.bottomNavigation.setOnItemSelectedListener { item ->
            val fragment: Fragment = when (item.itemId) {
                R.id.nav_dashboard -> DashboardFragment()
                R.id.nav_modules -> ModulesFragment()
                R.id.nav_tasks -> TasksFragment()
                R.id.nav_timetable -> TimetableFragment()
                R.id.nav_settings -> SettingsFragment()
                else -> DashboardFragment()
            }
            Log.d(tag, "Switching to ${fragment.javaClass.simpleName}")
            showFragment(fragment)
            true
        }
    }

    private fun showFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }
}

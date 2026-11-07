package com.bopape.studentassistant.ui.main

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.bopape.studentassistant.R
import com.bopape.studentassistant.databinding.ActivityFragmentHostBinding
import com.bopape.studentassistant.ui.modules.ModulesFragment
import com.bopape.studentassistant.ui.study.StudyPlannerFragment
import com.bopape.studentassistant.util.applySystemBarInsets

/**
 * A simple full-screen container (toolbar + one fragment) for the screens that
 * do not have their own bottom-navigation tab: Modules and the Study Planner.
 * Reusing the existing fragments avoids duplicating their screen logic.
 */
class FragmentHostActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFragmentHostBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFragmentHostBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.root.applySystemBarInsets()
        binding.toolbar.setNavigationOnClickListener { finish() }

        // The hosted fragments show their own large heading, so the toolbar only carries "back".
        val screen = intent.getStringExtra(EXTRA_SCREEN)
        binding.toolbar.title = ""
        binding.toolbar.navigationContentDescription =
            getString(if (screen == SCREEN_STUDY) R.string.study_planner_title else R.string.modules_title)

        if (savedInstanceState == null) {
            val fragment = if (screen == SCREEN_STUDY) StudyPlannerFragment() else ModulesFragment()
            supportFragmentManager.beginTransaction().replace(R.id.hostContainer, fragment).commit()
        }
    }

    companion object {
        private const val EXTRA_SCREEN = "screen"
        const val SCREEN_MODULES = "modules"
        const val SCREEN_STUDY = "study"

        fun intent(context: Context, screen: String) =
            Intent(context, FragmentHostActivity::class.java).putExtra(EXTRA_SCREEN, screen)
    }
}

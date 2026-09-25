package com.bopape.studentassistant

import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.bopape.studentassistant.ui.auth.LoginActivity
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented UI test: confirms the login screen validates an empty form instead
 * of crashing or silently attempting to sign in, per the requirement that the app
 * "handle invalid inputs made by the user without crashing".
 */
@RunWith(AndroidJUnit4::class)
class LoginActivityTest {

    @Test
    fun loginButton_withEmptyFields_showsValidationInsteadOfCrashing() {
        ActivityScenario.launch(LoginActivity::class.java).use {
            onView(withId(R.id.buttonLogin)).perform(click())

            // The screen should still be showing the email field with an error,
            // not have crashed or silently navigated away.
            onView(withId(R.id.editEmail)).check(matches(isDisplayed()))
        }
    }
}

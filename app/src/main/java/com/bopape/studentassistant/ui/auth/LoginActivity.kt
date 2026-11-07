package com.bopape.studentassistant.ui.auth

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.bopape.studentassistant.R
import com.bopape.studentassistant.databinding.ActivityLoginBinding
import com.bopape.studentassistant.ui.main.MainActivity
import com.bopape.studentassistant.util.applySystemBarInsets
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * Handles user login - email/password or Google single sign-on - and password
 * reset. Firebase Authentication stores and verifies the password hash itself
 * (the app never stores or sends a plain-text password to any server we
 * control) - see https://firebase.google.com/docs/auth (Google, 2026).
 */
class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignIn: GoogleSignInHelper
    private val tag = "LoginActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        // Branded launch screen with the app icon (Android 12+ splash API, back-ported).
        installSplashScreen()
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.root.applySystemBarInsets()

        auth = FirebaseAuth.getInstance()
        googleSignIn = GoogleSignInHelper(this)

        binding.buttonLogin.setOnClickListener { attemptLogin() }
        binding.buttonGoogleSignIn.setOnClickListener { signInWithGoogle() }
        binding.textForgotPassword.setOnClickListener { showPasswordReset() }
        binding.textGoToRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    override fun onStart() {
        super.onStart()
        // Already-logged-in users skip straight to the app.
        if (auth.currentUser != null) {
            Log.d(tag, "User already signed in, skipping login screen")
            goToMain()
        }
    }

    private fun attemptLogin() {
        val email = binding.editEmail.text.toString().trim()
        val password = binding.editPassword.text.toString()

        if (email.isEmpty()) {
            binding.layoutEmail.error = getString(R.string.error_email_required)
            return
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.layoutEmail.error = getString(R.string.error_email_invalid)
            return
        }
        binding.layoutEmail.error = null

        if (password.isEmpty()) {
            binding.layoutPassword.error = getString(R.string.error_password_required)
            return
        }
        binding.layoutPassword.error = null

        setLoading(true)
        Log.d(tag, "Attempting email login for $email")

        lifecycleScope.launch {
            try {
                auth.signInWithEmailAndPassword(email, password).await()
                Log.i(tag, "Email login succeeded for ${auth.currentUser?.uid}")
                finishSignIn()
            } catch (e: Exception) {
                setLoading(false)
                Log.w(tag, "Login failed", e)
                Toast.makeText(
                    this@LoginActivity,
                    getString(R.string.error_login_failed, e.localizedMessage ?: ""),
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun signInWithGoogle() {
        setLoading(true)
        lifecycleScope.launch {
            when (val result = googleSignIn.signIn(this@LoginActivity)) {
                is GoogleSignInHelper.Result.Success -> finishSignIn()
                is GoogleSignInHelper.Result.Cancelled -> setLoading(false)
                is GoogleSignInHelper.Result.Error -> {
                    setLoading(false)
                    Toast.makeText(this@LoginActivity, result.message, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    /** Applies the user's saved settings, registers push, then opens the app. */
    private suspend fun finishSignIn() {
        AccountSetup.afterSignIn(applicationContext)
        setLoading(false)
        goToMain()
    }

    /** "Forgot password?" - Firebase emails the user a secure reset link. */
    private fun showPasswordReset() {
        val input = TextInputEditText(this).apply {
            hint = getString(R.string.email_hint)
            setText(binding.editEmail.text)
            inputType = android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
        }
        val padding = resources.getDimensionPixelSize(R.dimen.dialog_padding)
        val container = android.widget.FrameLayout(this).apply {
            setPadding(padding, padding / 2, padding, 0)
            addView(input)
        }
        AlertDialog.Builder(this)
            .setTitle(R.string.reset_password_title)
            .setMessage(R.string.reset_password_message)
            .setView(container)
            .setPositiveButton(R.string.send) { _, _ ->
                val email = input.text.toString().trim()
                if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                    Toast.makeText(this, R.string.error_email_invalid, Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                auth.sendPasswordResetEmail(email).addOnCompleteListener { task ->
                    Log.i(tag, "Password reset email requested (success=${task.isSuccessful})")
                    // Same message either way, so the screen does not reveal which emails have accounts.
                    Toast.makeText(this, R.string.reset_password_sent, Toast.LENGTH_LONG).show()
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun setLoading(loading: Boolean) {
        binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        binding.buttonLogin.isEnabled = !loading
        binding.buttonGoogleSignIn.isEnabled = !loading
    }

    private fun goToMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}

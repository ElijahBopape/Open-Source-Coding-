package com.bopape.studentassistant.ui.auth

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.bopape.studentassistant.R
import com.bopape.studentassistant.databinding.ActivityRegisterBinding
import com.bopape.studentassistant.ui.main.MainActivity
import com.bopape.studentassistant.util.applySystemBarInsets
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * Handles account creation. Firebase Authentication salts and hashes the password
 * (scrypt) on Google's servers; this app never stores or transmits it in plain
 * text (see requirement 3.1 in the Part 1 design document).
 */
class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private lateinit var auth: FirebaseAuth
    private val tag = "RegisterActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.root.applySystemBarInsets()

        auth = FirebaseAuth.getInstance()

        binding.buttonRegister.setOnClickListener { attemptRegister() }
        binding.textGoToLogin.setOnClickListener { finish() }
    }

    private fun attemptRegister() {
        val name = binding.editName.text.toString().trim()
        val email = binding.editEmail.text.toString().trim()
        val password = binding.editPassword.text.toString()
        val confirmPassword = binding.editConfirmPassword.text.toString()

        if (name.isEmpty()) {
            binding.layoutName.error = getString(R.string.error_name_required)
            return
        }
        binding.layoutName.error = null

        if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.layoutEmail.error = getString(R.string.error_email_invalid)
            return
        }
        binding.layoutEmail.error = null

        if (password.length < 8) {
            binding.layoutPassword.error = getString(R.string.error_password_too_short)
            return
        }
        binding.layoutPassword.error = null

        if (password != confirmPassword) {
            binding.layoutConfirmPassword.error = getString(R.string.error_passwords_dont_match)
            return
        }
        binding.layoutConfirmPassword.error = null

        setLoading(true)
        Log.d(tag, "Attempting registration for $email")

        lifecycleScope.launch {
            try {
                auth.createUserWithEmailAndPassword(email, password).await()
                auth.currentUser?.updateProfile(
                    UserProfileChangeRequest.Builder().setDisplayName(name).build()
                )?.await()
                Log.i(tag, "Registration succeeded for ${auth.currentUser?.uid}")

                AccountSetup.afterSignIn(applicationContext)
                setLoading(false)
                startActivity(Intent(this@RegisterActivity, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                })
                finish()
            } catch (e: Exception) {
                setLoading(false)
                Log.w(tag, "Registration failed", e)
                Toast.makeText(
                    this@RegisterActivity,
                    getString(R.string.error_registration_failed, e.localizedMessage ?: ""),
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun setLoading(loading: Boolean) {
        binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        binding.buttonRegister.isEnabled = !loading
    }
}

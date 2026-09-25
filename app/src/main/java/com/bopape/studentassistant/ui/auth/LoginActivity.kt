package com.bopape.studentassistant.ui.auth

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bopape.studentassistant.R
import com.bopape.studentassistant.databinding.ActivityLoginBinding
import com.bopape.studentassistant.ui.main.MainActivity
import com.google.firebase.auth.FirebaseAuth

/**
 * Handles user login. Firebase Authentication stores and verifies the password
 * hash itself (client apps never see or transmit a plain-text password to any
 * server we control) - see https://firebase.google.com/docs/auth (Google, 2026).
 */
class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var auth: FirebaseAuth
    private val tag = "LoginActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        binding.buttonLogin.setOnClickListener { attemptLogin() }
        binding.textGoToRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
        binding.buttonGoogleSignIn.setOnClickListener {
            // Single sign-on is a PoE-only requirement (see assessment brief);
            // full Google Sign-In wiring is planned for the Final PoE submission.
            Toast.makeText(
                this,
                "Google Sign-In (SSO) will be enabled in the final submission",
                Toast.LENGTH_SHORT
            ).show()
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
        Log.d(tag, "Attempting login for $email")

        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                setLoading(false)
                if (task.isSuccessful) {
                    Log.i(tag, "Login succeeded for ${auth.currentUser?.uid}")
                    goToMain()
                } else {
                    Log.w(tag, "Login failed", task.exception)
                    Toast.makeText(
                        this,
                        getString(
                            R.string.error_login_failed,
                            task.exception?.localizedMessage ?: "unknown error"
                        ),
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
    }

    private fun setLoading(loading: Boolean) {
        binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        binding.buttonLogin.isEnabled = !loading
    }

    private fun goToMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}

package com.bopape.studentassistant.ui.auth

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bopape.studentassistant.R
import com.bopape.studentassistant.databinding.ActivityRegisterBinding
import com.bopape.studentassistant.ui.main.MainActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest

/**
 * Handles account creation. Firebase Authentication salts and hashes the password
 * before it ever leaves the device's TLS connection; this app never stores or
 * transmits it in plain text (see requirement 3.1 in the Part 1 design document).
 */
class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private lateinit var auth: FirebaseAuth
    private val tag = "RegisterActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

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

        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val profileUpdate = UserProfileChangeRequest.Builder()
                        .setDisplayName(name)
                        .build()
                    auth.currentUser?.updateProfile(profileUpdate)
                        ?.addOnCompleteListener {
                            setLoading(false)
                            Log.i(tag, "Registration succeeded for ${auth.currentUser?.uid}")
                            startActivity(Intent(this, MainActivity::class.java))
                            finish()
                        }
                } else {
                    setLoading(false)
                    Log.w(tag, "Registration failed", task.exception)
                    Toast.makeText(
                        this,
                        getString(
                            R.string.error_registration_failed,
                            task.exception?.localizedMessage ?: "unknown error"
                        ),
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

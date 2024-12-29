package nir.wolff.ui.profile

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import nir.wolff.databinding.ActivityProfileBinding
import nir.wolff.ui.auth.SignInActivity
import nir.wolff.ui.main.MainActivity

class ProfileActivity : AppCompatActivity() {
    private lateinit var binding: ActivityProfileBinding
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Profile"

        // Handle back button
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                finish()
            }
        })

        setupUI()
        loadUserProfile()
    }

    private fun setupUI() {
        binding.updateProfileButton.setOnClickListener {
            updateProfile()
        }

        binding.signOutButton.setOnClickListener {
            auth.signOut()
            startActivity(Intent(this, SignInActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            })
            finish()
        }

        binding.deleteAccountButton.setOnClickListener {
            deleteAccount()
        }
    }

    private fun loadUserProfile() {
        val user = auth.currentUser
        if (user == null) {
            startActivity(Intent(this, SignInActivity::class.java))
            finish()
            return
        }

        binding.emailInput.setText(user.email)
        binding.displayNameInput.setText(user.displayName)

        // Load additional user data from Firestore
        lifecycleScope.launch {
            try {
                val userDoc = firestore.collection("users")
                    .document(user.uid)
                    .get()
                    .await()

                if (userDoc.exists()) {
                    binding.phoneInput.setText(userDoc.getString("phone"))
                    binding.emergencyContactInput.setText(userDoc.getString("emergencyContact"))
                }
            } catch (e: Exception) {
                Toast.makeText(
                    this@ProfileActivity,
                    "Error loading profile: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun updateProfile() {
        val user = auth.currentUser ?: return
        val displayName = binding.displayNameInput.text.toString().trim()
        val phone = binding.phoneInput.text.toString().trim()
        val emergencyContact = binding.emergencyContactInput.text.toString().trim()

        lifecycleScope.launch {
            try {
                // Update display name in Firebase Auth
                val profileUpdates = UserProfileChangeRequest.Builder()
                    .setDisplayName(displayName)
                    .build()
                user.updateProfile(profileUpdates).await()

                // Update additional info in Firestore
                firestore.collection("users")
                    .document(user.uid)
                    .set(mapOf(
                        "displayName" to displayName,
                        "phone" to phone,
                        "emergencyContact" to emergencyContact
                    ))
                    .await()

                Toast.makeText(this@ProfileActivity, "Profile updated successfully", Toast.LENGTH_SHORT).show()
                finish()
            } catch (e: Exception) {
                Toast.makeText(
                    this@ProfileActivity,
                    "Error updating profile: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun deleteAccount() {
        val user = auth.currentUser ?: return

        lifecycleScope.launch {
            try {
                // Delete user data from Firestore
                firestore.collection("users")
                    .document(user.uid)
                    .delete()
                    .await()

                // Delete Firebase Auth account
                user.delete().await()

                Toast.makeText(this@ProfileActivity, "Account deleted successfully", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this@ProfileActivity, SignInActivity::class.java))
                finish()
            } catch (e: Exception) {
                Toast.makeText(
                    this@ProfileActivity,
                    "Error deleting account: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}

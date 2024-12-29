package nir.wolff.ui.auth

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import nir.wolff.R
import nir.wolff.databinding.ActivitySignInBinding
import nir.wolff.repository.UserRepository
import nir.wolff.ui.main.MainActivity

class SignInActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySignInBinding
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var auth: FirebaseAuth
    private val userRepository = UserRepository()
    
    companion object {
        private const val TAG = "SignInActivity"
    }

    private val signInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        Log.d(TAG, "Sign in result received: ${result.resultCode}")
        binding.progressBar.visibility = View.GONE
        binding.signInButton.isEnabled = true
        
        if (result.resultCode == RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                Log.d(TAG, "Sign-in successful: ${account.email}")
                account.idToken?.let { token ->
                    firebaseAuthWithGoogle(token, account.email ?: "", account.displayName ?: "")
                }
            } catch (e: ApiException) {
                Log.e(TAG, "Google sign in failed", e)
                showError("Google sign in failed: ${e.message}")
            }
        } else {
            Log.d(TAG, "Sign-in canceled or failed with resultCode: ${result.resultCode}")
            showError("Sign-in cancelled")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignInBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        auth = FirebaseAuth.getInstance()
        
        // Check if user is already signed in
        auth.currentUser?.let {
            Log.d(TAG, "User already signed in: ${it.email}")
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }
        
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.web_client_id))
            .requestEmail()
            .build()
            
        googleSignInClient = GoogleSignIn.getClient(this, gso)
        
        binding.signInButton.setOnClickListener {
            signIn()
        }
    }

    private fun signIn() {
        Log.d(TAG, "Starting sign-in process")
        binding.progressBar.visibility = View.VISIBLE
        binding.signInButton.isEnabled = false
        signInLauncher.launch(googleSignInClient.signInIntent)
    }

    private fun firebaseAuthWithGoogle(idToken: String, email: String, displayName: String) {
        Log.d(TAG, "firebaseAuthWithGoogle: starting authentication")
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnSuccessListener { authResult ->
                Log.d(TAG, "firebaseAuthWithGoogle: success")
                lifecycleScope.launch {
                    try {
                        userRepository.createOrUpdateUser(email, displayName)
                        startActivity(Intent(this@SignInActivity, MainActivity::class.java))
                        finish()
                    } catch (e: Exception) {
                        Log.e(TAG, "Error saving user data", e)
                        showError("Failed to save user data: ${e.message}")
                    }
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "firebaseAuthWithGoogle: failure", e)
                showError("Authentication failed: ${e.message}")
                binding.progressBar.visibility = View.GONE
                binding.signInButton.isEnabled = true
            }
    }

    private fun showError(message: String) {
        Log.e(TAG, "Error: $message")
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}

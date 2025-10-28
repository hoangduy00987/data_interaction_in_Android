package com.example.datainteraction.ui

import android.content.Intent
import android.os.Bundle
import android.util.Log // ✅ BƯỚC 1: Import thư viện Log
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.datainteraction.R
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider

class LoginActivity : AppCompatActivity() {

    // ✅ BƯỚC 2: Định nghĩa một TAG để lọc log
    private companion object {
        private const val TAG = "LoginActivity"
    }

    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient
    private val RC_SIGN_IN = 9001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.signin)
        Log.d(TAG, "onCreate: Activity started.")

        auth = FirebaseAuth.getInstance()

        // Kiểm tra nếu người dùng đã đăng nhập từ trước
        auth.currentUser?.let {
            Log.i(TAG, "onCreate: User already signed in. UID: ${it.uid}")
            Toast.makeText(this, "Already signed in as ${it.displayName}", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }

        // Cấu hình Google Sign-In
        Log.d(TAG, "onCreate: Configuring Google Sign-In.")
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)

        // Button đăng nhập
        val signInButton = findViewById<Button>(R.id.signInButton)
        signInButton.setOnClickListener {
            Log.d(TAG, "onClick: Sign-in button clicked.")
            signIn()
        }
    }

    private fun signIn() {
        Log.d(TAG, "signIn: Launching Google Sign-In intent.")
        val signInIntent = googleSignInClient.signInIntent
        startActivityForResult(signInIntent, RC_SIGN_IN)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        Log.d(TAG, "onActivityResult: request_code=$requestCode, result_code=$resultCode")

        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)
                Log.i(TAG, "onActivityResult: Google sign-in successful for ${account.email}")
                account.idToken?.let {
                    firebaseAuthWithGoogle(it)
                } ?: run {
                    Log.w(TAG, "onActivityResult: Google ID token is null!")
                    Toast.makeText(this, "Google ID token is null", Toast.LENGTH_SHORT).show()
                }
            } catch (e: ApiException) {
                // Đây là log quan trọng nhất để biết lý do đăng nhập Google thất bại
                Log.e(TAG, "onActivityResult: Google sign-in failed. Status code: ${e.statusCode}", e)
                Toast.makeText(this, "Google sign in failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        Log.d(TAG, "firebaseAuthWithGoogle: Attempting to authenticate with Firebase.")
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    Log.i(TAG, "firebaseAuthWithGoogle: Firebase authentication successful. UID: ${user?.uid}")
                    Toast.makeText(this, "Signed in as ${user?.displayName}", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                } else {
                    // Log này cũng rất quan trọng để biết tại sao Firebase từ chối token
                    Log.e(TAG, "firebaseAuthWithGoogle: Firebase authentication failed.", task.exception)
                    Toast.makeText(this, "Authentication failed", Toast.LENGTH_SHORT).show()
                }
            }
    }
}
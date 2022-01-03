package com.crystal.simpletinderapp

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import com.facebook.CallbackManager
import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.facebook.login.LoginResult
import com.facebook.login.widget.LoginButton
import com.google.firebase.auth.FacebookAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase

class LoginActivity: AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var callbackManager: CallbackManager
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        auth = Firebase.auth
        callbackManager = CallbackManager.Factory.create()

        initSignUpButton()
        initLoginButton()
        initEmailAndPWEditText()
        initFacebookLoginButton()

    }

    private fun initFacebookLoginButton() {
        val facebookLoginButton = findViewById<LoginButton>(R.id.facebookLoginButton)
        facebookLoginButton.setPermissions("email", "public_profile")
        facebookLoginButton.registerCallback(callbackManager, object :FacebookCallback<LoginResult>{
            override fun onSuccess(result: LoginResult) {
                //로그인 성공
                //파이어페이스에 넘긴다
                val credential = FacebookAuthProvider.getCredential(result.accessToken.token)
                auth.signInWithCredential(credential)
                    .addOnCompleteListener (this@LoginActivity){ task->
                        if(task.isSuccessful){
                            handleSuccessLogin()
                        }else{
                            Toast.makeText(this@LoginActivity, "페이스북 로그인이 실패했습니다.", Toast.LENGTH_SHORT).show()
                        }
                    }
            }

            override fun onCancel() {

            }

            override fun onError(error: FacebookException?) {
                //로그인 실패
                Toast.makeText(this@LoginActivity, "페이스북 로그인 실패",Toast.LENGTH_SHORT).show()
            }

        })
    }


    private fun initSignUpButton() {
        val signupButton = findViewById<Button>(R.id.signUpButton)
        signupButton.setOnClickListener {
            val email = getEmail()
            val pw = getPW()
            auth.createUserWithEmailAndPassword(email, pw)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        Toast.makeText(this, "회원가입에 성공했습니다. 로그인버튼을 눌러 로그인해주세요.", Toast.LENGTH_LONG)
                            .show()
                    } else {
                        Toast.makeText(this, "회원가입에 실패했습니다. 이미 회원가입되었거나 비밀번호는 6자리 이상사용해야함", Toast.LENGTH_LONG).show()
                    }
                }

        }
    }

    private fun initLoginButton() {
        val loginButton = findViewById<Button>(R.id.loginButton)
        loginButton.setOnClickListener {
            val email = getEmail()
            val pw = getPW()
            auth.signInWithEmailAndPassword(email, pw)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        handleSuccessLogin()
                    } else {
                        Toast.makeText(this, "로그인에 실패했습니다. 이메일과 비밀번호를 확인해주세요", Toast.LENGTH_LONG)
                            .show()
                    }
                }
        }
    }

    private fun getEmail() = findViewById<EditText>(R.id.emailEditText).text.toString()

    private fun getPW() = findViewById<EditText>(R.id.passwordEditText).text.toString()


    private fun initEmailAndPWEditText() {
        val emailEditText = findViewById<EditText>(R.id.emailEditText)
        val pWEditText = findViewById<EditText>(R.id.passwordEditText)
        val loginButton = findViewById<Button>(R.id.loginButton)
        val signUpButton = findViewById<Button>(R.id.signUpButton)
        emailEditText.addTextChangedListener {
            val enable = emailEditText.text.isNotEmpty() && pWEditText.text.isNotEmpty()
            loginButton.isEnabled = enable
            signUpButton.isEnabled = enable
        }
        pWEditText.addTextChangedListener {
            val enable = pWEditText.text.isNotEmpty() && emailEditText.text.isNotEmpty()
            loginButton.isEnabled = enable
            signUpButton.isEnabled = enable
        }

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        callbackManager.onActivityResult(requestCode, resultCode, data)
    }

    private fun handleSuccessLogin(){
        if(auth.currentUser == null){
            Toast.makeText(this, "로그인 실패", Toast.LENGTH_SHORT).show()
            return
        }
        val userId = auth.currentUser?.uid.orEmpty()
        //firebase realtime data base 에 저장하기.
        val currentUserDB = Firebase.database.reference.child(DBKey.USERS).child(userId)
        val user = mutableMapOf<String, Any>()
        user[DBKey.USER_ID] = userId
        currentUserDB.updateChildren(user)

        finish()
    }

}
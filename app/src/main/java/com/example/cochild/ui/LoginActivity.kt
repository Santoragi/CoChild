package com.example.cochild

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.login) // activity_login.xml 연결

        // View 연결
        val loginButton: Button = findViewById(R.id.loginButton)    // 로그인 버튼
        val signUpButton: Button = findViewById(R.id.signUpButton)  // 회원가입 버튼
        val usernameInput: EditText = findViewById(R.id.login)      // 이메일
        val passwordInput: EditText = findViewById(R.id.password)   // 비밀번호

        // 로그인 버튼 클릭 이벤트
        loginButton.setOnClickListener {
            // UI 정보 가져오기
            val username = usernameInput.text.toString()    // 이메일
            val password = passwordInput.text.toString()    // 비밀번호

            // 로그인 처리 로직 (예: Firebase 로그인 )
            doSignin(username, password)
        }

        // 회원가입 버튼 클릭 이벤트
        signUpButton.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }

    }

    // 로그인
    private fun doSignin(email: String, password: String) {
        auth = FirebaseAuth.getInstance()

        if (email.isNotEmpty() && password.isNotEmpty()) {
            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        // 로그인 성공시 MainHome으로 이동
                        startActivity(Intent(this, MainHomeActivity::class.java))
                        finish() // 로그인 화면 종료
                    } else {
                        Toast.makeText(this, "로그인 실패: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                    }
                }
        } else {
            Toast.makeText(this, "이메일과 비밀번호를 입력해주세요.", Toast.LENGTH_SHORT).show()
        }

    }
}

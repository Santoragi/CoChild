package com.example.cochild

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : AdminBaseActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        FirebaseApp.initializeApp(this)
        FirebaseAuth.getInstance()
        FirebaseFirestore.getInstance()

        auth = FirebaseAuth.getInstance()



        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        redirectActivity()  // 로그인 정보에 따라 액티비티 실행

    }

    /**
     * 로그인 상태를 확인하고 적절한 액티비티로 전환
     */
    private fun redirectActivity() {
        auth = FirebaseAuth.getInstance()
        val currentUser = auth.currentUser
        Log.d("MainActivity", "로그인 상태 확인 시작")
        if (currentUser == null) {
            // 로그인 정보가 없으면 LoginActivity로 이동
            Log.d("MainActivity", "사용자 로그인 정보 없음. LoginActivity로 이동")
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
        } else {
            // 로그인 정보가 있으면 MainHome으로 이동
            Log.d("MainActivity", "사용자 로그인 상태 확인: UID=${currentUser.uid}")
            val intent = Intent(this, MainHomeActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

}
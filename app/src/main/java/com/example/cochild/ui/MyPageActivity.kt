package com.example.cochild

import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat.startActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.cochild.R
import com.example.cochild.RegisterActivity
import com.example.cochild.models.User
import com.example.cochild.BaseActivity
import com.example.cochild.LoginActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

class MyPageActivity : BaseActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var storage: FirebaseStorage

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.mypage)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.mypage)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
        storage = FirebaseStorage.getInstance()

        //상단바
        setupToolbar()

        // 뒤로가기 아이콘 클릭 시 MainHome 액티비티로 돌아가기
        val backIcon: ImageView = findViewById(R.id.backIcon)
        backIcon.setOnClickListener {
            // 뒤로 가기
            onBackPressed()
        }

        // 현재 사용자 UID 가져오기
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(this, "로그인 정보가 없습니다.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // 사용자 데이터 불러오기
        val userId = currentUser.uid
        loadUserData(userId)

        // 회원 수정 버튼
        findViewById<Button>(R.id.updateButton).setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            intent.putExtra("IS_EDIT_MODE", true)
            startActivity(intent)
        }

        // 로그아웃 버튼
        findViewById<Button>(R.id.logoutButton).setOnClickListener {
            doSignOut()
        }

        // 회원탈퇴 버튼 클릭 이벤트
        findViewById<Button>(R.id.deleteAccountButton).setOnClickListener {
            deleteUserAccount()
        }

    }

    override fun onResume() {
        super.onResume()
        val currentUser = auth.currentUser
        if (currentUser != null) {
            loadUserData(currentUser.uid) // Firestore에서 사용자 데이터 다시 로드
        }
    }

    /**
     * Firestore에서 사용자 데이터를 불러오는 함수
     */
    private fun loadUserData(userId: String) {
        firestore.collection("Users").document(userId).get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    // Firestore에서 데이터 가져오기
                    val user = document.toObject(User::class.java)
                    if (user != null) {
                        displayUserData(user)
                    } else {
                        Toast.makeText(this, "사용자 데이터를 찾을 수 없습니다.", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this, "사용자 데이터를 찾을 수 없습니다.", Toast.LENGTH_SHORT).show()
                    Log.e("MyPageActivity", "Document does not exist")
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "데이터 불러오기 실패: ${e.message}", Toast.LENGTH_SHORT).show()
                Log.e("MyPageActivity", "Error loading user data", e)
            }
    }

    /**
     * 사용자 데이터를 UI에 표시하는 함수
     */
    private fun displayUserData(user: User) {
        findViewById<TextView>(R.id.nameValue).text = user.name
        findViewById<TextView>(R.id.genderValue).text = user.gender
        findViewById<TextView>(R.id.dobValue).text = user.birthday
        findViewById<TextView>(R.id.phoneValue).text = user.phone
        findViewById<TextView>(R.id.emailValue).text = user.email

        val profilePhotoView = findViewById<ImageView>(R.id.profilePhoto)

        // 프로필 사진 로드
        if (user.profilePhoto.isNullOrEmpty()) {
            profilePhotoView.setImageResource(R.drawable.baseline_person_24) // 기본 이미지
        } else {
            loadImageFromUrl(user.profilePhoto, profilePhotoView)
        }
    }

    /**
     * 프로필 이미지 불러오는 함수
     */
    private fun loadImageFromUrl(url: String?, imageView: ImageView) {
        if (url == null || url.trim() == "null") {
            // URL이 null이면 기본 이미지를 설정하고 종료
            runOnUiThread {
                imageView.setImageResource(R.drawable.baseline_person_24)
            }
            return
        }

        // URL이 유효한 경우에만 이미지 로드 시도
        thread {
            try {
                val connection = URL(url).openConnection() as HttpURLConnection
                connection.doInput = true
                connection.connect()
                val inputStream = connection.inputStream
                val bitmap = BitmapFactory.decodeStream(inputStream)

                // UI 스레드에서 이미지 설정
                runOnUiThread {
                    imageView.setImageBitmap(bitmap)
                }
            } catch (e: Exception) {
                // 로드 실패 시 기본 이미지를 설정하고 메시지 출력
                runOnUiThread {
                    imageView.setImageResource(R.drawable.baseline_person_24) // 기본 이미지 설정
                    Toast.makeText(this, "이미지 로드 실패", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }


    /**
     * 로그아웃 함수
     */
    private fun doSignOut() {
        try {
            auth.signOut() // Firebase 로그아웃
            Toast.makeText(this, "로그아웃 성공", Toast.LENGTH_SHORT).show()
            navigateToLogin()   // 로그인 화면으로 이동
        } catch (e: Exception) {
            // 로그아웃 실패 처리
            Toast.makeText(this, "로그아웃 실패: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * 회원탈퇴 로직
     */
    private fun deleteUserAccount() {
        val currentUser = auth.currentUser // FirebaseUser 가져오기
        if (currentUser == null) {
            Toast.makeText(this, "로그인 정보가 없습니다.", Toast.LENGTH_SHORT).show()
            return
        }

        val userId = currentUser.uid // FirebaseUser의 UID 가져오기

        // 알림창 표시
        AlertDialog.Builder(this)
            .setTitle("회원 탈퇴")
            .setMessage("정말로 회원 탈퇴를 진행하시겠습니까? 모든 데이터가 삭제됩니다.")
            .setPositiveButton("탈퇴") { _, _ ->
                proceedWithAccountDeletion(currentUser, userId)
            }
            .setNegativeButton("취소", null)
            .show()
    }

    private fun proceedWithAccountDeletion(currentUser: FirebaseUser, userId: String) {
        // Firestore 사용자 문서 가져오기
        firestore.collection("Users").document(userId).get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val profilePhotoUrl = document.getString("profilePhoto")

                    // Firestore 사용자 문서 삭제
                    firestore.collection("Users").document(userId).delete()
                        .addOnSuccessListener {
                            if (!profilePhotoUrl.isNullOrEmpty() && profilePhotoUrl != "null") {
                                // Storage에서 프로필 사진 삭제
                                val storageRef = storage.getReferenceFromUrl(profilePhotoUrl)
                                storageRef.delete()
                                    .addOnSuccessListener {
                                        deleteAuthUser(currentUser) // Auth 삭제로 진행
                                    }
                                    .addOnFailureListener {
                                        Toast.makeText(this, "프로필 사진 삭제 실패", Toast.LENGTH_SHORT).show()
                                        deleteAuthUser(currentUser) // 실패해도 Auth 삭제 진행
                                    }
                            } else {
                                // 프로필 사진이 없는 경우 바로 Auth 삭제로 진행
                                deleteAuthUser(currentUser)
                            }
                        }
                        .addOnFailureListener {
                            Toast.makeText(this, "Firestore 사용자 정보 삭제 실패", Toast.LENGTH_SHORT).show()
                        }
                } else {
                    Toast.makeText(this, "사용자 데이터를 찾을 수 없습니다.", Toast.LENGTH_SHORT).show()
                    deleteAuthUser(currentUser) // Firestore 데이터가 없어도 Auth 삭제 진행
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Firestore 사용자 데이터 불러오기 실패", Toast.LENGTH_SHORT).show()
            }
    }

    /**
     * Firebase Authentication 사용자 계정 삭제
     */
    private fun deleteAuthUser(user: FirebaseUser) {
        user.delete()
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Toast.makeText(this, "회원탈퇴 성공", Toast.LENGTH_SHORT).show()
                    navigateToLogin()
                } else {
                    Toast.makeText(this, "회원탈퇴 실패: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                    Log.e("MyPageActivity", "회원탈퇴 실패", task.exception)
                }
            }
    }


    /**
     * 로그인 화면으로 이동
     */
    private fun navigateToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}


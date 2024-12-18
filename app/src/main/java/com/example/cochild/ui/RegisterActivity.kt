package com.example.cochild

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.cochild.models.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread
import android.graphics.BitmapFactory

class RegisterActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var storage: FirebaseStorage

    private var selectedImageUri: Uri? = null // 사용자가 선택한 이미지 URI
    private var isEditMode: Boolean = false // 수정 모드 여부
    private var currentUserId: String? = null // 현재 사용자 ID
    private var currentProfilePhotoUrl: String? = null // 기존 프로필 사진 URL

    // 갤러리 권한 요청
    companion object {
        private const val REQUEST_CODE_PERMISSION = 2000
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.register)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
        storage = FirebaseStorage.getInstance()

        checkPermissions()  // 갤러리 권한 요청

        // 수정 모드 확인
        isEditMode = intent.getBooleanExtra("IS_EDIT_MODE", false)
        currentUserId = auth.currentUser?.uid

        setupUI()

        // 이미지 선택 버튼 클릭 이벤트
        findViewById<Button>(R.id.uploadPhotoButton).setOnClickListener {
            selectProfileImage()
        }

        // 회원가입 또는 수정 완료 버튼 클릭 이벤트
        findViewById<Button>(R.id.signUpButton).setOnClickListener {
            val name = findViewById<EditText>(R.id.nameEditText).text.toString()
            val birthday = findViewById<EditText>(R.id.dobEditText).text.toString()
            val phone = findViewById<EditText>(R.id.phoneEditText).text.toString()
            val email = findViewById<EditText>(R.id.emailEditText).text.toString()
            val password = findViewById<EditText>(R.id.passwordEditText).text.toString()
            val genderId = findViewById<RadioGroup>(R.id.genderRadioGroup).checkedRadioButtonId
            val gender = if (genderId != -1) findViewById<RadioButton>(genderId).text.toString() else ""

            // 필드 값 검증
            if (!validateFields(name, gender, birthday, phone, email, password)) {
                return@setOnClickListener
            }


            if (isEditMode) {
                updateUserProfile(name, gender, birthday, phone, email, password)
            } else {
                // 회원가입 로직
                if (selectedImageUri != null) {
                    uploadImageToStorage { imageUrl ->
                        doSignUp(
                            profilePhoto = Uri.parse(imageUrl),
                            name = name,
                            gender = gender,
                            birthday = birthday,
                            phone = phone,
                            email = email,
                            password = password
                        )
                    }
                } else {
                    doSignUp(
                        profilePhoto = null,
                        name = name,
                        gender = gender,
                        birthday = birthday,
                        phone = phone,
                        email = email,
                        password = password
                    )
                }
            }
        }
    }

    /**
     * 회원가입 관련 함수
     * Firebase Auth 회원가입
     */
    private fun doSignUp(profilePhoto: Uri?, name: String, gender: String, birthday: String, phone: String, email: String, password: String) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val userId = auth.currentUser?.uid
                    if (userId == null) {
                        Toast.makeText(this, "유저 ID를 가져올 수 없습니다.", Toast.LENGTH_SHORT).show()
                        Log.e("RegisterActivity", "유저 ID가 null입니다.")
                        return@addOnCompleteListener
                    }
                    // user 객체 생성
                    val user = User(
                        profilePhoto = profilePhoto.toString(),
                        name = name,
                        gender = gender,
                        birthday = birthday,
                        phone = phone,
                        email = email,
                        teams = emptyList()
                    )

                    // Firestore에 사용자 정보 저장
                    firestore.collection("Users")
                        .document(userId)
                        .set(user)
                        .addOnCompleteListener { dbTask ->
                            if (dbTask.isSuccessful) {
                                Toast.makeText(this, "회원가입 성공", Toast.LENGTH_SHORT).show()
                                Log.d("RegisterActivity", "Firestore에 데이터 저장 성공")
                                finish() // 회원가입 후 종료
                            } else {
                                Toast.makeText(
                                    this,
                                    "FireStore 저장 실패: ${dbTask.exception?.message}",
                                    Toast.LENGTH_SHORT
                                ).show()
                                Log.e("RegisterActivity", "Firestore 저장 실패", dbTask.exception)
                            }
                        }
                } else {
                    Toast.makeText(this, "회원가입 실패: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                    Log.e("RegisterActivity", "Firebase Auth 회원가입 실패", task.exception)
                }
            }
    }


    /**
     * 필드 값 검증
     */
    private fun validateFields(
        name: String,
        gender: String,
        birthday: String,
        phone: String,
        email: String,
        password: String
    ): Boolean {
        // 필드가 비어 있는지 확인
        if (name.isEmpty() || gender.isEmpty() || birthday.isEmpty() || phone.isEmpty() || email.isEmpty() || password.isEmpty()) {
            if (name.isEmpty()) {
                Toast.makeText(this, "이름을 입력해주세요.", Toast.LENGTH_SHORT).show()
            } else if (gender.isEmpty()) {
                Toast.makeText(this, "성별을 선택해주세요.", Toast.LENGTH_SHORT).show()
            } else if (birthday.isEmpty()) {
                Toast.makeText(this, "생년월일을 입력해주세요.", Toast.LENGTH_SHORT).show()
            } else if (phone.isEmpty()) {
                Toast.makeText(this, "전화번호를 입력해주세요.", Toast.LENGTH_SHORT).show()
            } else if (email.isEmpty()) {
                Toast.makeText(this, "이메일을 입력해주세요.", Toast.LENGTH_SHORT).show()
            } else if (password.isEmpty()) {
                Toast.makeText(this, "비밀번호를 입력해주세요.", Toast.LENGTH_SHORT).show()
            }
            return false
        }
        return true
    }



    /**
     * UI 초기 설정
     */
    private fun setupUI() {
        val titleView = findViewById<TextView>(R.id.headerText)
        val buttonView = findViewById<Button>(R.id.signUpButton)

        if (isEditMode) {
            titleView.text = "정보 수정"
            buttonView.text = "수정 완료"
            loadUserData()
        } else {
            titleView.text = "회원가입"
            buttonView.text = "회원가입"
        }
    }

    /**
     * Firestore에서 사용자 데이터 로드 및 UI 업데이트
     */
    private fun loadUserData() {
        currentUserId?.let { userId ->
            firestore.collection("Users").document(userId).get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        val user = document.toObject(User::class.java)
                        user?.let {
                            findViewById<EditText>(R.id.nameEditText).setText(it.name)
                            findViewById<EditText>(R.id.dobEditText).setText(it.birthday)
                            findViewById<EditText>(R.id.phoneEditText).setText(it.phone)
                            findViewById<EditText>(R.id.emailEditText).setText(it.email)
                            findViewById<EditText>(R.id.passwordEditText).setText("") // 비밀번호는 UI에 표시하지 않음

                            // 성별 라디오 버튼 체크
                            when (it.gender) {
                                "남자" -> findViewById<RadioButton>(R.id.maleRadioButton).isChecked = true
                                "여자" -> findViewById<RadioButton>(R.id.femaleRadioButton).isChecked = true
                            }

                            // 프로필 이미지 설정
                            currentProfilePhotoUrl = it.profilePhoto
                            val profilePhotoView = findViewById<ImageView>(R.id.profilePhoto)
                            if (!it.profilePhoto.isNullOrEmpty()) {
                                loadImageFromUrl(it.profilePhoto, profilePhotoView)
                            }
                        }
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "사용자 정보 불러오기 실패: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    /**
     * 사용자 프로필 수정
     */
    private fun updateUserProfile(name: String, gender: String, birthday: String, phone: String, email: String, password: String) {
        currentUserId?.let { userId ->
            val currentUser = auth.currentUser
            if (currentUser == null || currentUser.email != email) {
                Toast.makeText(this, "이메일이 옳지 않습니다", Toast.LENGTH_SHORT).show()
                return
            }

            // 비밀번호 확인 추가
            currentUser.let { user ->
                auth.signInWithEmailAndPassword(user.email!!, password)
                    .addOnCompleteListener { task ->
                        if (!task.isSuccessful) {
                            Toast.makeText(this, "비밀번호가 옳지 않습니다", Toast.LENGTH_SHORT).show()
                            return@addOnCompleteListener
                        }

                        // 비밀번호 확인 후 업데이트 진행
                        val userUpdates = mutableMapOf<String, Any>(
                            "name" to name,
                            "gender" to gender,
                            "birthday" to birthday,
                            "phone" to phone
                        )

                        if (selectedImageUri != null) {
                            uploadImageToStorage { imageUrl ->
                                currentProfilePhotoUrl?.let { deleteImageFromStorage(it) }
                                userUpdates["profilePhoto"] = imageUrl
                                saveUserUpdates(userId, userUpdates)
                            }
                        } else {
                            saveUserUpdates(userId, userUpdates)
                        }
                    }
            }
        }
    }


    /**
     * 사용자 데이터를 Firestore에 저장
     */
    private fun saveUserUpdates(userId: String, updates: Map<String, Any>) {
        firestore.collection("Users").document(userId).update(updates)
            .addOnSuccessListener {
                Toast.makeText(this, "정보가 수정되었습니다.", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "정보 수정 실패: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    // Firebase Storage에 이미지 업로드
    private fun uploadImageToStorage(onSuccess: (String) -> Unit) {
        val userId = auth.currentUser?.uid ?: "temp_user" // userId가 없는 경우 임시 ID 사용
        val timestamp = System.currentTimeMillis() // 현재 시간을 가져와서 고유 파일 이름 생성
        val storageRef = storage.getReference("profile_images/${userId}_$timestamp.jpg") // 고유한 파일 이름 생성

        selectedImageUri?.let { imageUri ->
            storageRef.putFile(imageUri)
                .addOnSuccessListener {
                    storageRef.downloadUrl.addOnSuccessListener { uri ->
                        onSuccess(uri.toString()) // 업로드된 URL 반환
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "이미지 업로드 실패: ${e.message}", Toast.LENGTH_SHORT).show()
                    Log.e("RegisterActivity", "Firebase Storage 이미지 업로드 실패", e)
                }
        } ?: run {
            Toast.makeText(this, "이미지를 선택하지 않았습니다.", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Firebase Storage에서 기존 이미지 삭제
     */
    private fun deleteImageFromStorage(imageUrl: String) {
        val storageRef = storage.getReferenceFromUrl(imageUrl)
        storageRef.delete()
            .addOnSuccessListener {
                Log.d("RegisterActivity", "기존 이미지 삭제 완료: $imageUrl")
            }
            .addOnFailureListener { e ->
                Log.e("RegisterActivity", "기존 이미지 삭제 실패: ${e.message}", e)
            }
    }

    /**
     * URL에서 이미지를 로드하여 ImageView에 설정
     */
    private fun loadImageFromUrl(url: String?, imageView: ImageView) {
        if (url.isNullOrEmpty()) return
        thread {
            try {
                val connection = URL(url).openConnection() as HttpURLConnection
                connection.doInput = true
                connection.connect()
                val bitmap = BitmapFactory.decodeStream(connection.inputStream)
                runOnUiThread {
                    imageView.setImageBitmap(bitmap)
                }
            } catch (e: Exception) {
                Log.e("RegisterActivity", "이미지 로드 실패: ${e.message}")
            }
        }
    }


    // 갤러리에서 프로필 이미지 선택
    private fun selectProfileImage() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, 1001) // requestCode: 1001
    }

    // 선택한 이미지 결과 처리
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1001 && resultCode == RESULT_OK) {
            selectedImageUri = data?.data
            val profilePhoto = findViewById<ImageView>(R.id.profilePhoto)
            profilePhoto.setImageURI(selectedImageUri) // 선택한 이미지를 ImageView에 표시
        }
    }




    // 갤러리 접근 권한 요청하는 함수
    private fun checkPermissions() {
        val permission = android.Manifest.permission.READ_EXTERNAL_STORAGE
        if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(permission), REQUEST_CODE_PERMISSION)
        }
    }

}

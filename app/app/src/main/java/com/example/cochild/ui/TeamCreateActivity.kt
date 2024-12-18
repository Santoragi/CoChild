package com.example.cochild

import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.cochild.utils.TeamIdHelper
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

class TeamCreateActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var storage: FirebaseStorage

    private var selectedImageUri: Uri? = null // 사용자가 선택한 팀 프로필 이미지 URI
    private var uploadedImageUrl: String = "" // Firebase Storage에 업로드된 이미지 URL
//    private var teamId: String = "" // 팀 ID를 초기화

    private var currentProfilePhotoUrl: String? = null // 현재 저장된 프로필 사진 URL
    private var isEditMode: Boolean = false // 수정 모드 여부
    private var teamId: String? = null // 수정할 팀의 ID

    companion object {
        private const val REQUEST_CODE_PERMISSION = 2000
        private const val REQUEST_CODE_IMAGE_PICK = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.team_create)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
        storage = FirebaseStorage.getInstance()

        // 뒤로가기 아이콘 클릭 시 MainHome 액티비티로 돌아가기
        val backIcon: ImageView = findViewById(R.id.backIcon)
        backIcon.setOnClickListener {
            // 뒤로 가기
            onBackPressed()
        }

        isEditMode = intent.getBooleanExtra("IS_EDIT_MODE", false)
        teamId = intent.getStringExtra("TEAM_ID")

        setupUI()

        checkPermissions() // 갤러리 권한 요청

        // 이미지 추가 버튼 클릭 이벤트
        findViewById<Button>(R.id.addProfilePhotoButton).setOnClickListener {
            selectProfileImage()
        }

        // 팀 생성/수정 버튼 클릭 이벤트
        findViewById<Button>(R.id.createTeamButton).setOnClickListener {
            // UI 정보 가져오기 (팀 이름, 카테고리, 설명)
            val teamName: String = findViewById<EditText>(R.id.teamNameEditText).text.toString()
            val teamCategory: String = findViewById<EditText>(R.id.teamCategoryEditText).text.toString()
            val teamDescription: String = findViewById<EditText>(R.id.teamDescriptionEditText).text.toString()

            // 모든 필드 검증
            if (teamName.isEmpty() || teamCategory.isEmpty() || teamDescription.isEmpty()) {
                Toast.makeText(this, "모든 필드를 채워주세요!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (isEditMode) {
                updateTeamInfo(teamName, teamCategory, teamDescription)
            } else {
                teamId = firestore.collection("teams").document().id // 새로운 팀 ID 생성
                if (selectedImageUri != null) {
                    uploadImageToStorage(selectedImageUri!!) { imageUrl ->
                        createTeam(Uri.parse(imageUrl), teamName, teamCategory, teamDescription)
                    }
                } else {
                    createTeam(null, teamName, teamCategory, teamDescription)
                }
            }

        }
    }

    /**
     * UI 초기 설정
     */
    private fun setupUI() {
        val titleView = findViewById<TextView>(R.id.headerText)
        val buttonView = findViewById<Button>(R.id.createTeamButton)

        if (isEditMode) {
            titleView.text = "정보 수정"
            buttonView.text = "수정 완료"
//            currentTeamId = TeamIdHelper.getTeamId(this)
//            loadTeamData()
            teamId?.let { loadTeamInfo(it) }
        } else {
            titleView.text = "팀 생성"
            buttonView.text = "팀 생성"
        }
    }

    /**
     * 팀 생성 관련 함수
     */
    private fun createTeam(profilePhoto: Uri?, name: String, category: String, description: String) {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            Toast.makeText(this, "로그인 정보가 없습니다.", Toast.LENGTH_SHORT).show()
            return
        }

        // 팀 데이터 객체 생성
        val teamData = hashMapOf(
            "teamId" to teamId, // 명시적으로 생성한 teamId 추가
            "name" to name,
            "category" to category,
            "profileImage" to (profilePhoto?.toString() ?: ""), // 이미지가 없을 경우 빈 문자열 처리
            "introduction" to description,
            "admin" to userId,
            "members" to listOf(userId) // 팀 생성자는 자동으로 팀 멤버로 추가
        )

        // Firestore에 팀 데이터 저장
        firestore.collection("teams")
            .document(teamId!!) // 명시적으로 생성한 teamId 사용
            .set(teamData)
            .addOnSuccessListener {
                Toast.makeText(this, "팀 생성 성공", Toast.LENGTH_SHORT).show()
                Log.d("TeamCreateActivity", "팀 생성 성공: $teamId")

                updateUserTeamsField(userId, teamId!!, name) // user 테이블 수정

                // 팀 생성 후 메인홈 페이지로 이동
                val intent = Intent(this, MainHomeActivity::class.java)
                intent.putExtra("TEAM_ID", teamId) // 팀 ID 전달
                startActivity(intent)
                finish() // 팀 생성 후 종료
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "팀 생성 실패: ${e.message}", Toast.LENGTH_SHORT).show()
                Log.e("TeamCreateActivity", "팀 생성 실패", e)
            }
    }

    /**
     * 팀 생성 시 팀장 uid user 테이블의 Teams에 추가
     */
    private fun updateUserTeamsField(userId: String, teamId: String, teamName: String) {
//        val teamInfo = mapOf("teamId" to teamId, "teamName" to teamName)
        val teamInfo = mapOf("name" to teamName, "teamId" to teamId)

        // Firestore 트랜잭션을 통해 배열 업데이트
        firestore.collection("Users").document(userId)
            .update("teams", FieldValue.arrayUnion(teamInfo))
            .addOnSuccessListener {
//                Toast.makeText(this, "사용자 팀 정보 업데이트 성공", Toast.LENGTH_SHORT).show()
                Log.d("TeamCreateActivity", "사용자 팀 정보 업데이트 성공: $teamInfo")
            }
            .addOnFailureListener { e ->
//                Toast.makeText(this, "사용자 팀 정보 업데이트 실패: ${e.message}", Toast.LENGTH_SHORT).show()
                Log.e("TeamCreateActivity", "사용자 팀 정보 업데이트 실패", e)
            }
    }

    /**
     * 수정 시 팀 정보 불러오기
     */
    private fun loadTeamInfo(teamId: String) {
        firestore.collection("teams").document(teamId).get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val teamName = document.getString("name") ?: ""
                    val teamCategory = document.getString("category") ?: ""
                    val teamDescription = document.getString("introduction") ?: ""
                    currentProfilePhotoUrl = document.getString("profileImage")

                    findViewById<EditText>(R.id.teamNameEditText).setText(teamName)
                    findViewById<EditText>(R.id.teamCategoryEditText).setText(teamCategory)
                    findViewById<EditText>(R.id.teamDescriptionEditText).setText(teamDescription)

                    currentProfilePhotoUrl?.let { loadImageFromUrl(it, findViewById(R.id.teamProfilePhoto)) }
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "팀 정보를 불러오지 못했습니다.", Toast.LENGTH_SHORT).show()
            }
    }

    /**
     * 팀 정보 수정 후 업데이트
     */
    private fun updateTeamInfo(name: String, category: String, description: String) {
        // teamId가 null인지 확인
        if (teamId.isNullOrEmpty()) {
            Toast.makeText(this, "유효하지 않은 팀 ID입니다.", Toast.LENGTH_SHORT).show()
            return
        }

        val updates = mutableMapOf<String, Any>(
            "name" to name,
            "category" to category,
            "introduction" to description
        )

        if (selectedImageUri != null) {
            // 새로운 이미지가 선택된 경우
            uploadImageToStorage(selectedImageUri!!) { imageUrl ->
                // 새로운 이미지 업로드 성공 후 기존 이미지 삭제 및 Firestore 업데이트
                deleteCurrentImageAndSave(imageUrl, updates)
            }
        } else {
            // 이미지 변경 없이 업데이트
            saveUpdates(updates)
        }
    }

    /**
     * 팀 정보 수정 후 저장
     * Firestore에 팀 정보 업데이트 저장
     */
    private fun saveUpdates(updates: Map<String, Any>) {
        teamId?.let { id ->
            firestore.collection("teams").document(id).update(updates)
                .addOnSuccessListener {
                    Toast.makeText(this, "팀 정보가 수정되었습니다.", Toast.LENGTH_SHORT).show()
                    Log.d("TeamCreateActivity", "Firestore 업데이트 성공: $updates")
                    finish()
                }
                .addOnFailureListener { e ->
                    Log.e("TeamCreateActivity", "Firestore 업데이트 실패: ${e.message}")
                    Toast.makeText(this, "팀 정보 수정에 실패했습니다: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    /**
     * Firebase Storage에 이미지 업로드
     */
    private fun uploadImageToStorage(imageUri: Uri, onSuccess: (String) -> Unit) {
        if (teamId.isNullOrEmpty()) {
            Log.e("TeamCreateActivity", "teamId가 null이거나 비어 있습니다.")
            Toast.makeText(this, "유효하지 않은 팀 ID입니다.", Toast.LENGTH_SHORT).show()
            return
        }

        // 현재 시간(타임스탬프)을 가져와 파일 이름에 추가
        val timestamp = System.currentTimeMillis()
        val storageRef = storage.getReference("team_images/${teamId}_$timestamp.jpg") // 팀 ID와 타임스탬프를 파일 이름으로 사용

        storageRef.putFile(imageUri)
            .addOnSuccessListener { taskSnapshot ->
                // 파일 업로드 성공 후 URL 가져오기
                storageRef.downloadUrl.addOnSuccessListener { uri ->
                    val uploadedImageUrl = uri.toString()
                    Log.d("TeamCreateActivity", "이미지 업로드 성공: $uploadedImageUrl")
                    onSuccess(uploadedImageUrl) // 성공 시 콜백 호출
                }.addOnFailureListener { e ->
                    Log.e("TeamCreateActivity", "업로드된 이미지 URL 가져오기 실패: ${e.message}")
                    Toast.makeText(this, "이미지 URL을 가져오는 데 실패했습니다.", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Log.e("TeamCreateActivity", "이미지 업로드 실패: ${e.message}")
                Toast.makeText(this, "이미지 업로드에 실패했습니다.", Toast.LENGTH_SHORT).show()
            }
    }

    /**
     * 기존 이미지 삭제 후 Firestore 업데이트
     */
    private fun deleteCurrentImageAndSave(newImageUrl: String, updates: MutableMap<String, Any>) {
        if (!currentProfilePhotoUrl.isNullOrEmpty()) {
            // 기존 이미지가 있을 경우 삭제
            storage.getReferenceFromUrl(currentProfilePhotoUrl!!).delete()
                .addOnSuccessListener {
                    Log.d("TeamCreateActivity", "기존 이미지 삭제 완료: $currentProfilePhotoUrl")
                    // 삭제 완료 후 새로운 이미지 URL로 업데이트 진행
                    updates["profileImage"] = newImageUrl
                    saveUpdates(updates)
                }
                .addOnFailureListener { e ->
                    Log.e("TeamCreateActivity", "기존 이미지 삭제 실패: ${e.message}")
                    Toast.makeText(this, "기존 이미지 삭제 중 문제가 발생했습니다.", Toast.LENGTH_SHORT).show()
                    // 기존 이미지 삭제에 실패해도 새로운 이미지 URL로 업데이트 진행
                    updates["profileImage"] = newImageUrl
                    saveUpdates(updates)
                }
        } else {
            // 기존 이미지가 없을 경우 바로 업데이트 진행
            updates["profileImage"] = newImageUrl
            saveUpdates(updates)
        }
    }

    // 이미지 수정 시 기존 이미지는 삭제
    private fun deleteImageFromStorage(imageUrl: String) {
        storage.getReferenceFromUrl(imageUrl).delete()
            .addOnSuccessListener { Log.d("TeamCreateActivity", "기존 이미지 삭제 완료: $imageUrl") }
            .addOnFailureListener { Log.e("TeamCreateActivity", "기존 이미지 삭제 실패", it) }
    }

    private fun loadImageFromUrl(url: String, imageView: ImageView) {
        thread {
            try {
                val connection = URL(url).openConnection() as HttpURLConnection
                connection.doInput = true
                connection.connect()
                val bitmap = BitmapFactory.decodeStream(connection.inputStream)
                runOnUiThread { imageView.setImageBitmap(bitmap) }
            } catch (e: Exception) {
                Log.e("TeamCreateActivity", "이미지 로드 실패", e)
            }
        }
    }

    // 갤러리에서 프로필 이미지 선택
    private fun selectProfileImage() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, REQUEST_CODE_IMAGE_PICK)
    }

    // 선택한 이미지 결과 처리
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_IMAGE_PICK && resultCode == RESULT_OK) {
            selectedImageUri = data?.data
            val profilePhoto = findViewById<ImageView>(R.id.teamProfilePhoto)
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

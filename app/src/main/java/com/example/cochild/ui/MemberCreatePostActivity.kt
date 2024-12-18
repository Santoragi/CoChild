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
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.cochild.AdminTeamBoardActivity
import com.example.cochild.ui.MemberBaseActivity
import com.example.cochild.MemberReadPostActivity
import com.example.cochild.MemberTeamBoardActivity
import com.example.cochild.R
import com.example.cochild.models.Post
import com.example.cochild.models.Team
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Calendar
import kotlin.concurrent.thread

class MemberCreatePostActivity: MemberBaseActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var storage: FirebaseStorage

    private var selectedImageUri: Uri? = null
    private var currentPostPhotoUrl: String? = null


    private var currentPost: Post? = null

    private var userId: String = ""
    private var postId: String = ""
    private var teamId: String = "" // 기본 팀 ID

    companion object {
        private const val REQUEST_CODE_PERMISSION = 2000
        private const val REQUEST_CODE_IMAGE_PICK = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_post)

        // Firebase 인스턴스 초기화
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
        storage = FirebaseStorage.getInstance()

        // 갤러리 권한 확인
        checkPermissions()

        // Setup bottom navigation
        setupBottomNavigationView(R.id.nav_board, teamId)

        // 게시글 상세 정보 및 모드 설정
        setupUIMode()
    }

    private fun setupUIMode() {
        // 인텐트에서 전달받은 값 확인
        teamId = intent.getStringExtra("TEAM_ID") ?: teamId
        postId = intent.getStringExtra("POST_ID") ?: ""

        val titleView = findViewById<TextView>(R.id.post_banner)
        titleView.setOnClickListener{

            val intent = Intent(this, MemberTeamBoardActivity::class.java)
            intent.putExtra("TEAM_ID", teamId)
            startActivity(intent)

        }
        val buttonView = findViewById<Button>(R.id.post_button)

        // 이미지 업로드 버튼 설정
        findViewById<Button>(R.id.upload_post_photo_button).setOnClickListener {
            selectPostImage()
        }

        // 모드에 따른 UI 설정

            // 새 게시글 모드
        setupSaveButton()

    }



    private fun setupSaveButton() {
        findViewById<Button>(R.id.post_button).setOnClickListener {
            val title = findViewById<EditText>(R.id.post_title).text.toString()
            val body = findViewById<EditText>(R.id.post_body).text.toString()
            val createdTime = getCurrentDateTime()


            createNewPost(title, body, createdTime)

        }
    }

    private fun createNewPost(title: String, body: String, createdTime: String) {
        // 새 게시글 ID 생성
        postId = firestore.collection("Posts").document().id
        userId = auth.currentUser?.uid.toString()

        // 이미지가 선택된 경우 먼저 업로드
        if (selectedImageUri != null) {
            uploadImageToStorage { imageUrl ->
                saveNewPost(title, body, createdTime, imageUrl)
            }
        } else {
            saveNewPost(title, body, createdTime, null)
        }
    }

    private fun saveNewPost(title: String, body: String, createdTime: String, imageUrl: String?) {
        val post = Post(
            teamId = teamId,
            userId = userId,
            postId = postId,
            postPhoto = imageUrl ?: "",
            title = title,
            body = body,
            createdTime = createdTime
        )

        firestore.collection("Posts")
            .document(postId)
            .set(post)
            .addOnSuccessListener {
                // 팀의 게시글 배열에 추가
                firestore.collection("teams")
                    .document(teamId)
                    .update("posts", FieldValue.arrayUnion(post))
                    .addOnSuccessListener {
                        navigateToPostDetail()
                    }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "게시글 생성 실패: ${e.message}", Toast.LENGTH_SHORT).show()
                navigateToPostList()
            }
    }


    private fun navigateToPostList() {
        val intent = Intent(this, AdminTeamBoardActivity::class.java)
        intent.putExtra("TEAM_ID", teamId)
        startActivity(intent)
        finish()
    }

    private fun navigateToPostDetail() {
        val intent = Intent(this, MemberReadPostActivity::class.java)
        intent.putExtra("POST_ID", postId)
        intent.putExtra("TEAM_ID", teamId)
        startActivity(intent)
        finish()
    }



    private fun uploadImageToStorage(onSuccess: (String) -> Unit) {
        val timestamp = System.currentTimeMillis()
        val storageRef = storage.getReference("post_images/${postId}_$timestamp.jpg")

        selectedImageUri?.let { imageUri ->
            storageRef.putFile(imageUri)
                .addOnSuccessListener {
                    storageRef.downloadUrl.addOnSuccessListener { uri ->
                        onSuccess(uri.toString())
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "이미지 업로드 실패: ${e.message}", Toast.LENGTH_SHORT).show()
                    Log.e("AdminCreatePostActivity", "Firebase Storage 이미지 업로드 실패", e)
                }
        } ?: run {
            Toast.makeText(this, "이미지를 선택하지 않았습니다.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadImageFromUrl(url: String?, imageView: ImageView) {
        if (url.isNullOrEmpty() || url.trim() == "null") return

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
                Log.e("AdminCreatePostActivity", "이미지 로드 실패: ${e.message}")
            }
        }
    }

    private fun selectPostImage() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, REQUEST_CODE_IMAGE_PICK)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_IMAGE_PICK && resultCode == RESULT_OK) {
            selectedImageUri = data?.data
            val postPhoto = findViewById<ImageView>(R.id.post_photo)
            postPhoto.setImageURI(selectedImageUri)
            //postPhoto.scaleType = ImageView.ScaleType.CENTER_CROP
        }
    }

    private fun checkPermissions() {
        val permission = android.Manifest.permission.READ_EXTERNAL_STORAGE
        if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(permission), REQUEST_CODE_PERMISSION)
        }
    }

    private fun getCurrentDateTime(): String {
        val calendar = Calendar.getInstance()
        val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm")
        return formatter.format(calendar.time)
    }
}
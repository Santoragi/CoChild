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
import com.example.cochild.models.Notice
import com.example.cochild.models.Team
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*
import kotlin.concurrent.thread

class AdminCreateNoticeActivity : AdminBaseActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var storage: FirebaseStorage

    private var selectedImageUri: Uri? = null
    private var currentNoticePhotoUrl: String? = null

    private var isEditMode = false
    private var currentNotice: Notice? = null

    private var authorId: String = ""
    private var noticeId: String = ""
    private var teamId: String = ""
    private var dueDate: String = ""

    companion object {
        private const val REQUEST_CODE_PERMISSION = 2000
        private const val REQUEST_CODE_IMAGE_PICK = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_create_notice)

        // Initialize Firebase instances
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
        storage = FirebaseStorage.getInstance()

        // Check gallery permissions
        checkPermissions()

        // Get team and notice details from intent
        teamId = intent.getStringExtra("TEAM_ID").toString()
        if (teamId.isEmpty() || teamId == "null") {
            Toast.makeText(this, "유효한 팀 ID가 없습니다.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Setup UI based on mode (create or edit)
        setupUIMode()

        // Setup bottom navigation
        setupBottomNavigationView(R.id.nav_notice, teamId)
    }

    private fun setupUIMode() {
        noticeId = intent.getStringExtra("NOTICE_ID") ?: ""
        isEditMode = intent.getBooleanExtra("EDIT_MODE", false)

        val titleView = findViewById<TextView>(R.id.notice_banner)
        val buttonView = findViewById<Button>(R.id.notice_button)
        val dateButton: Button = findViewById(R.id.notice_due_date)

        // Setup date picker
        dateButton.setOnClickListener { showDatePicker(dateButton) }

        // Setup image upload button
        findViewById<Button>(R.id.upload_notice_photo_button).setOnClickListener {
            selectNoticeImage()
        }

        // Configure UI for edit or create mode
        if (isEditMode) {
            titleView.text = "정보 수정"
            buttonView.text = "수정 완료"

            // Load existing notice data
            loadNoticeData { notice ->
                currentNotice = notice
                populateUIWithNoticeData(notice)
                setupSaveButton(notice)
            }
        } else {
            // Create mode - directly setup save button
            setupSaveButton(null)
            dueDate = getCurrentDateTime()
            dateButton.text = dueDate
        }
    }

    private fun populateUIWithNoticeData(notice: Notice) {
        findViewById<EditText>(R.id.notice_title).setText(notice.title)
        findViewById<EditText>(R.id.notice_body).setText(notice.body)
        findViewById<Button>(R.id.notice_due_date).text = notice.dueDate
        dueDate = notice.dueDate
        currentNoticePhotoUrl = notice.noticePhoto

        val noticePhoto = findViewById<ImageView>(R.id.notice_photo)
        loadImageFromUrl(notice.noticePhoto, noticePhoto)
    }

    private fun setupSaveButton(existingNotice: Notice?) {
        findViewById<Button>(R.id.notice_button).setOnClickListener {
            val title = findViewById<EditText>(R.id.notice_title).text.toString()
            val body = findViewById<EditText>(R.id.notice_body).text.toString()
            val createdTime = getCurrentDateTime()

            if (isEditMode) {
                updateExistingNotice(title, body, createdTime, existingNotice)
            } else {
                createNewNotice(title, body, createdTime)
            }
        }
    }

    private fun createNewNotice(title: String, body: String, createdTime: String) {
        // Generate new notice ID
        noticeId = firestore.collection("Notices").document().id
        authorId = auth.currentUser?.uid.toString()

        // If image is selected, upload first
        if (selectedImageUri != null) {
            uploadImageToStorage { imageUrl ->
                saveNewNotice(title, body, createdTime, imageUrl)
            }
        } else {
            saveNewNotice(title, body, createdTime, null)
        }
    }

    private fun saveNewNotice(title: String, body: String, createdTime: String, imageUrl: String?) {
        val notice = Notice(
            noticeId = noticeId,
            authorId = authorId,
            teamId = teamId,
            noticePhoto = imageUrl ?: "",
            title = title,
            body = body,
            createdTime = createdTime,
            dueDate = dueDate
        )

        firestore.collection("Notices")
            .document(noticeId)
            .set(notice)
            .addOnSuccessListener {
                // Add notice to team's notices array
                firestore.collection("teams")
                    .document(teamId)
                    .update("notices", FieldValue.arrayUnion(notice))
                    .addOnSuccessListener {
                        navigateToNoticeList()
                    }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "공지사항 생성 실패: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun updateExistingNotice(title: String, body: String, createdTime: String, existingNotice: Notice?) {
        existingNotice ?: return

        // Prepare update data
        val noticeUpdates = mutableMapOf<String, Any>(
            "title" to title,
            "body" to body,
            "dueDate" to dueDate,
            "createdTime" to createdTime
        )

        // If new image selected, upload first
        if (selectedImageUri != null) {
            uploadImageToStorage { imageUrl ->
                // Delete old image if exists
                currentNoticePhotoUrl?.let { deleteImageFromStorage(it) }
                noticeUpdates["noticePhoto"] = imageUrl
                saveNoticeUpdate(noticeUpdates)
            }
        } else {
            saveNoticeUpdate(noticeUpdates)
        }
    }

    private fun saveNoticeUpdate(noticeUpdates: Map<String, Any>) {


        // 업데이트할 Notice 객체 생성
        val updatedNotice = Notice(
            noticeId = noticeId,
            authorId = currentNotice?.authorId ?: "",
            teamId = teamId,
            noticePhoto = noticeUpdates["noticePhoto"] as? String ?: currentNotice?.noticePhoto ?: "",
            title = noticeUpdates["title"] as String,
            body = noticeUpdates["body"] as String,
            createdTime = noticeUpdates["createdTime"] as String,
            dueDate = noticeUpdates["dueDate"] as String
        )


        // Update notice in Notices collection
        firestore.collection("Notices")
            .document(noticeId)
            .set(updatedNotice)
            .addOnSuccessListener {
                // Update notice in team's notices array
                updateTeamNoticesArray(updatedNotice)
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "공지사항 수정 실패: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }



    private fun updateTeamNoticesArray(updatedNotice: Notice) {
        val teamDocRef = firestore.collection("teams").document(teamId)

        teamDocRef.get()
            .addOnSuccessListener { documentSnapshot ->
                val team = documentSnapshot.toObject(Team::class.java)
                val currentNotice = team?.notices?.find { it.noticeId == noticeId }

                if (currentNotice != null) {
                    // Remove old notice and add updated notice
                    teamDocRef.update(
                        "notices",
                        FieldValue.arrayRemove(currentNotice),
                        "notices",
                        FieldValue.arrayUnion(updatedNotice)
                    )
                        .addOnSuccessListener {
                            navigateToNoticeDetail()
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(this, "팀 공지사항 업데이트 실패: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                }
            }
    }

    private fun navigateToNoticeList() {
        val intent = Intent(this, AdminTeamNoticeActivity::class.java)
        intent.putExtra("TEAM_ID", teamId)
        startActivity(intent)
        finish()
    }

    private fun navigateToNoticeDetail() {
        val intent = Intent(this, AdminReadNoticeActivity::class.java)
        intent.putExtra("NOTICE_ID", noticeId)
        intent.putExtra("TEAM_ID", teamId)
        startActivity(intent)
        finish()
    }

    private fun loadNoticeData(onComplete: (Notice) -> Unit) {
        firestore.collection("teams")
            .document(teamId)
            .get()
            .addOnSuccessListener { documentSnapshot ->
                if (documentSnapshot.exists()) {
                    val team = documentSnapshot.toObject(Team::class.java)
                    val currentNotice = team?.notices?.find { it.noticeId == noticeId }

                    if (currentNotice != null) {
                        onComplete(currentNotice)
                    } else {
                        Toast.makeText(this, "공지사항을 찾을 수 없습니다.", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this, "팀 정보를 찾을 수 없습니다.", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "공지사항 데이터를 불러오지 못했습니다: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }



    // ... (rest of the existing methods like checkPermissions, selectNoticeImage, etc. remain the same)


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

    private fun uploadImageToStorage(onSuccess: (String) -> Unit) {
        val timestamp = System.currentTimeMillis()
        val storageRef = storage.getReference("notice_images/${noticeId}_$timestamp.jpg")

        selectedImageUri?.let { imageUri ->
            storageRef.putFile(imageUri)
                .addOnSuccessListener {
                    storageRef.downloadUrl.addOnSuccessListener { uri ->
                        onSuccess(uri.toString()) // 업로드된 URL 반환
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "이미지 업로드 실패: ${e.message}", Toast.LENGTH_SHORT).show()
                    Log.e("AdminCreateNotcieActivity", "Firebase Storage 이미지 업로드 실패", e)
                }
        } ?: run {
            Toast.makeText(this, "이미지를 선택하지 않았습니다.", Toast.LENGTH_SHORT).show()
        }
    }


//    ------------------------------------------------- 건드리지 마 ------------------------------------------------------------

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
                    imageView.layoutParams.height = bitmap.height
                    imageView.layoutParams.width = bitmap.width
                }
            } catch (e: Exception) {
                Log.e("AdminCreateNoticeActivity", "이미지 로드 실패: ${e.message}")
            }
        }

    }



    // 갤러리에서 공지사항 이미지 선택
    private fun selectNoticeImage() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, AdminCreateNoticeActivity.REQUEST_CODE_IMAGE_PICK)
    }

    // 선택한 이미지 결과 처리
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == AdminCreateNoticeActivity.REQUEST_CODE_IMAGE_PICK && resultCode == RESULT_OK) {
            selectedImageUri = data?.data
            val noticePhoto = findViewById<ImageView>(R.id.notice_photo)
            noticePhoto.setImageURI(selectedImageUri) // 선택한 이미지를 ImageView에 표시
        }
    }



    // 갤러리 접근 권한 요청하는 함수
    private fun checkPermissions() {
        val permission = android.Manifest.permission.READ_EXTERNAL_STORAGE
        if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(permission),
                AdminCreateNoticeActivity.REQUEST_CODE_PERMISSION
            )
        }
    }

    // 현재 시간 가져오기
    private fun getCurrentDateTime(): String {
        // 현재 시간 가져오기
        val calendar = Calendar.getInstance()

        // 날짜와 시간 형식 지정
        val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm")
        return formatter.format(calendar.time)
    }


    // MaterialDatePicker 표시 및 선택 날짜 처리
    private fun showDatePicker(dateButton: Button) {
        // DatePicker 빌드
        val datePicker =
            MaterialDatePicker.Builder.datePicker()
                .setTitleText("날짜 선택")
                .build()

        // DatePicker 표시
        datePicker.show(supportFragmentManager, "DATE_PICKER")

        // 날짜 선택 시 처리
        datePicker.addOnPositiveButtonClickListener { date ->
            val selectedDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(date))
            dateButton.text = selectedDate // 버튼 텍스트에 날짜 표시
            dueDate = selectedDate // 선택된 날짜를 저장

        }
    }

}
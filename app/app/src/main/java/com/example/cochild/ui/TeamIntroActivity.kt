package com.example.cochild

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread
import android.graphics.BitmapFactory
import android.widget.Button
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue

class TeamIntroActivity : AppCompatActivity() {

    private lateinit var firestore: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var teamId: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.team_intro)

        // Firestore 초기화
        firestore = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        // 이전 Activity에서 전달받은 teamId 가져오기
        teamId = intent.getStringExtra("TEAM_ID") ?: ""
        if (teamId.isEmpty()) {
            Toast.makeText(this, "팀 ID를 가져올 수 없습니다.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // 팀 데이터 로드
        loadTeamData()

        // 뒤로가기 버튼 동작
        findViewById<ImageView>(R.id.backIcon).setOnClickListener {
            onBackPressed()
        }

        // 가입신청 버튼 동작
        findViewById<Button>(R.id.joinButton).setOnClickListener{
            applyToTeam(teamId)
        }
    }

    /**
     * Firestore에서 팀 데이터를 가져와 UI 업데이트
     */
    private fun loadTeamData() {
        firestore.collection("teams").document(teamId).get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    // Firestore에서 가져온 팀 정보
                    val teamName = document.getString("name") ?: "알 수 없음"
                    val teamCategory = document.getString("category") ?: "알 수 없음"
                    val teamIntroduction = document.getString("introduction") ?: "소개 없음"
                    val teamProfileImage = document.getString("profileImage") ?: ""
                    val teamAdminId = document.getString("admin") ?: "알 수 없음" // 변경: 팀장 ID로 사용
                    val teamMembers = (document.get("members") as? List<*>)?.size ?: 0

                    // UI에 값 업데이트
                    findViewById<TextView>(R.id.headerText).text = teamName
                    findViewById<TextView>(R.id.teamCategoryValue).text = teamCategory
                    findViewById<TextView>(R.id.memberCountValue).text = "${teamMembers}명"
                    findViewById<TextView>(R.id.teamDescriptionContent).text = teamIntroduction

                    // 팀 이미지 로드
                    val profileImageView = findViewById<ImageView>(R.id.teamProfilePhoto)
                    loadImageFromUrl(teamProfileImage, profileImageView)

                    // 팀장 이름 가져오기 (추가된 부분)
                    loadAdminName(teamAdminId)
                } else {
                    Toast.makeText(this, "팀 데이터를 불러올 수 없습니다.", Toast.LENGTH_SHORT).show()
                    Log.e("TeamIntroActivity", "Firestore 문서가 존재하지 않음")
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "데이터 불러오기 실패: ${e.message}", Toast.LENGTH_SHORT).show()
                Log.e("TeamIntroActivity", "Firestore 데이터 가져오기 실패", e)
            }
    }

    /**
     * 팀장 이름 가져오기 함수 (추가된 부분)
     */
    private fun loadAdminName(adminId: String) {
        firestore.collection("Users").document(adminId).get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val adminName = document.getString("name") ?: "알 수 없음"
                    findViewById<TextView>(R.id.teamLeaderValue).text = adminName // 팀장 이름 업데이트
                } else {
                    findViewById<TextView>(R.id.teamLeaderValue).text = "알 수 없음"
                    Log.e("TeamIntroActivity", "팀장 정보가 없습니다.")
                }
            }
            .addOnFailureListener { e ->
                findViewById<TextView>(R.id.teamLeaderValue).text = "알 수 없음"
                Log.e("TeamIntroActivity", "팀장 정보 로드 실패: ${e.message}")
            }
    }

    /**
     * URL에서 이미지를 로드하여 ImageView에 설정하는 함수
     */
    private fun loadImageFromUrl(url: String?, imageView: ImageView) {
        if (url.isNullOrEmpty() || url.trim() == "null") {
            // URL이 비어있으면 기본 이미지를 설정
            runOnUiThread {
                imageView.setImageResource(R.drawable.baseline_add_photo_alternate_24)
            }
            return
        }

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
                // 로드 실패 시 기본 이미지를 설정
                runOnUiThread {
                    imageView.setImageResource(R.drawable.baseline_add_photo_alternate_24)
                    Toast.makeText(this, "이미지 로드 실패", Toast.LENGTH_SHORT).show()
                }
                Log.e("TeamIntroActivity", "이미지 로드 실패", e)
            }
        }
    }

    /**
     * 가입신청한 사용자의 정보를 firestore에 업로드
     */
    private fun applyToTeam(teamId: String) {
        val userId = auth.currentUser?.uid ?: return

        // 팀 데이터 가져오기
        firestore.collection("teams").document(teamId).get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val preMembers = document.get("preMembers") as? List<String> ?: emptyList()

                    // preMembers에 현재 사용자의 uid가 있는지 확인
                    if (preMembers.contains(userId)) {
                        Toast.makeText(this, "이미 가입신청을 했습니다.", Toast.LENGTH_SHORT).show()
                        return@addOnSuccessListener
                    }

                    // 가입 신청 추가
                    firestore.collection("teams").document(teamId)
                        .update("preMembers", FieldValue.arrayUnion(userId)) // preMembers에 추가
                        .addOnSuccessListener {
                            Toast.makeText(this, "가입신청이 완료되었습니다.", Toast.LENGTH_SHORT).show()
                            Log.d("TeamIntroActivity", "Successfully applied to team: $teamId")
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(this, "가입신청에 실패했습니다.", Toast.LENGTH_SHORT).show()
                            Log.e("TeamIntroActivity", "Error applying to team: ${e.message}", e)
                        }
                } else {
                    Toast.makeText(this, "팀 정보를 불러올 수 없습니다.", Toast.LENGTH_SHORT).show()
                    Log.e("TeamIntroActivity", "Document does not exist")
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "팀 정보를 가져오는 데 실패했습니다: ${e.message}", Toast.LENGTH_SHORT).show()
                Log.e("TeamIntroActivity", "Error fetching team data: ${e.message}", e)
            }





        val intent = Intent(this, MainHomeActivity::class.java)
        startActivity(intent)

    }

}

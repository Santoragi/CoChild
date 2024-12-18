package com.example.cochild

import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.cochild.models.Member
import com.example.cochild.ui.MemberBaseActivity
import com.example.cochild.ui.adapter.MemberProfileAdapter
import com.example.cochild.utils.TeamIdHelper
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

class MemberTeamHomeActivity : MemberBaseActivity() {

    private lateinit var adminId: String
    private lateinit var memberList: RecyclerView
    private lateinit var memberProfileAdapter: MemberProfileAdapter
    private lateinit var firestore: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var teamId: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_member_team_home)

        // Window Insets 설정
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.member_team_home)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, 0, systemBars.right, systemBars.bottom) // 상단 패딩 제거
            insets
        }

        firestore = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        // Firestore에서 전달받은 팀 ID 가져오기
        teamId = TeamIdHelper.getTeamId(this).toString()
        //teamId = "6sYbSMOwX727g6w3Senb"
        if (teamId.isNullOrEmpty()) {
            Toast.makeText(this, "팀 ID가 없습니다.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }


        // 팀 데이터 로드
        loadTeamData(teamId)

        // RecyclerView 설정
        memberList = findViewById(R.id.member_list)
        memberProfileAdapter = MemberProfileAdapter(this)
        memberList.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        memberList.adapter = memberProfileAdapter

        // 하단바 설정
        setupBottomNavigationView(R.id.nav_home, teamId)
        // 상단바 설정
        setupToolbar()
    }

    // 이 액티비티로 돌아올 때, 데이터 새로 로드
    override fun onResume() {
        super.onResume()

        val teamId = intent.getStringExtra("TEAM_ID")
        if (teamId.isNullOrEmpty()) {
            Toast.makeText(this, "팀 ID가 없습니다.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        Log.d("AdminTeamHomeActivity", "onResume: Reloading team data for TEAM_ID: $teamId")

        // 팀 데이터를 다시 로드
        loadTeamData(teamId)
    }


    /**
     * Firestore에서 팀 데이터를 가져오는 함수
     */
    private fun loadTeamData(teamId: String) {
        Log.d("AdminTeamHomeActivity", "Loading team data for TEAM_ID: $teamId")
        firestore.collection("teams").document(teamId).get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val teamName = document.getString("name") ?: "알 수 없는 팀 이름"
                    val teamIntro = document.getString("introduction") ?: "소개 없음"
                    adminId = document.getString("admin").toString()
                    val memberIds = document.get("members") as? List<String> ?: emptyList()

                    //Log.d("AdminTeamHomeActivity", "Team Name: $teamName, Admin ID: $adminId, Members: $memberIds")

                    // UI 업데이트
                    findViewById<TextView>(R.id.team_name).text = teamName
                    findViewById<TextView>(R.id.team_intro_body).text = teamIntro

                    // 팀장 정보 가져오기
                    if (!adminId.isNullOrEmpty()) {
                        loadAdminData(adminId)
                    }

                    // 멤버 ID 리스트 필터링 후 로드
                    val filteredMemberIds = memberIds.filter { it != adminId }
                    //Log.d("AdminTeamHomeActivity", "Filtered Members: $filteredMemberIds")
                    loadMemberData(filteredMemberIds)
                } else {
                    Toast.makeText(this, "팀 데이터를 찾을 수 없습니다.", Toast.LENGTH_SHORT).show()
                    Log.e("AdminTeamHomeActivity", "문서가 존재하지 않음")
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "데이터를 불러오지 못했습니다: ${e.message}", Toast.LENGTH_SHORT).show()
                Log.e("AdminTeamHomeActivity", "Firestore 오류: ${e.message}")
            }
    }


    /**
     * Firestore에서 팀장의 정보를 가져오는 함수
     */
    private fun loadAdminData(adminId: String) {
        Log.d("AdminTeamHomeActivity", "Loading admin data for Admin ID: $adminId")
        firestore.collection("Users").document(adminId).get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val adminName = document.getString("name") ?: "알 수 없는 이름"
                    val adminProfilePhotoUrl = document.getString("profilePhoto")

                    //Log.d("AdminTeamHomeActivity", "Admin Name: $adminName, Profile Photo URL: $adminProfilePhotoUrl")

                    // UI 업데이트
                    findViewById<TextView>(R.id.leader_name).text = adminName
                    if (!adminProfilePhotoUrl.isNullOrEmpty()) {
                        loadImageFromUrl(adminProfilePhotoUrl, findViewById(R.id.member_profile))
                    }
                } else {
                    Toast.makeText(this, "팀장 정보를 찾을 수 없습니다.", Toast.LENGTH_SHORT).show()
                    //Log.e("AdminTeamHomeActivity", "문서가 존재하지 않음")
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "팀장 데이터를 불러오지 못했습니다: ${e.message}", Toast.LENGTH_SHORT).show()
                //Log.e("AdminTeamHomeActivity", "Firestore 오류: ${e.message}")
            }
    }

    /**
     * Firestore에서 멤버 데이터를 가져오는 함수
     */

    private fun loadMemberData(memberIds: List<String>) {
        Log.d("AdminTeamHomeActivity", "Loading member data for IDs: $memberIds")
        if (memberIds.isEmpty()) {
            Log.d("AdminTeamHomeActivity", "No members found to load.")
            return
        }

        firestore.collection("Users")
            .whereIn(FieldPath.documentId(), memberIds) // 수정된 부분
            .get()
            .addOnSuccessListener { documents ->
                val members = documents.map { document ->
                    val name = document.getString("name") ?: "이름 없음"
                    val profilePhoto = document.getString("profilePhoto") ?: ""
                    Member(name, profilePhoto).also {
                        Log.d("AdminTeamHomeActivity", "Loaded Member: $it")
                    }
                }
                memberProfileAdapter.updateMembers(members)
                Log.d("AdminTeamHomeActivity", "Total Members Loaded: ${members.size}")
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "멤버 데이터를 불러오지 못했습니다: ${e.message}", Toast.LENGTH_SHORT).show()
                Log.e("AdminTeamHomeActivity", "Firestore 오류: ${e.message}")
            }
    }



    /**
     * URL에서 이미지를 불러오는 함수
     */
    private fun loadImageFromUrl(url: String?, imageView: ImageView) {
        if (url.isNullOrEmpty()) {
            imageView.setImageResource(R.drawable.ic_launcher_foreground)
            Log.e("MemberTeamHomeActivity", "URL이 비어 있습니다.")
            return
        }

        thread {
            try {
                val connection = URL(url).openConnection() as HttpURLConnection
                connection.doInput = true
                connection.connect()
                val inputStream = connection.inputStream
                val bitmap = BitmapFactory.decodeStream(inputStream)

                runOnUiThread {
                    imageView.setImageBitmap(bitmap)
                }
            } catch (e: Exception) {
                Log.e("AdminTeamHomeActivity", "이미지 로드 실패: ${e.message}", e)
                runOnUiThread {
                    imageView.setImageResource(R.drawable.ic_launcher_foreground)
                }
            }
        }
    }
}

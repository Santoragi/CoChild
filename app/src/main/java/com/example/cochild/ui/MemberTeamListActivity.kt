package com.example.cochild

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.cochild.models.Member
import com.example.cochild.ui.MemberBaseActivity
import com.example.cochild.ui.adapter.MemberTeamListAdapter
import com.example.cochild.utils.TeamIdHelper
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore

class MemberTeamListActivity : MemberBaseActivity() {

    private lateinit var teamMemberList: RecyclerView
    private lateinit var memberTeamListAdapter: MemberTeamListAdapter

    private lateinit var firestore: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_member_team_list)

        // Window Insets 설정
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.member_team_list)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val teamId = TeamIdHelper.getTeamId(this)
        if (teamId.isNullOrEmpty()) {
            Toast.makeText(this, "팀 ID가 없습니다.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        //하단바 설정
        setupBottomNavigationView(R.id.nav_home, teamId)
        //상단바
        setupToolbar()
        firestore = FirebaseFirestore.getInstance()

        // 팀 멤버 RecyclerView 설정
        teamMemberList = findViewById(R.id.team_member_list)
        teamMemberList.layoutManager = LinearLayoutManager(this)


        // 팀 멤버 및 가입 신청 리스트 로드
        loadTeamMembers(teamId)

    }

    private fun loadTeamMembers(teamId: String) {
        firestore.collection("teams").document(teamId).get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val memberIds = document.get("members") as? List<String> ?: emptyList()

                    if (memberIds.isEmpty()) {
                        Toast.makeText(this, "팀원이 없습니다.", Toast.LENGTH_SHORT).show()
                        return@addOnSuccessListener
                    }

                    // 관리자 ID를 추출하고 멤버 목록에서 제외
                    val adminId = memberIds.firstOrNull() // 0번 인덱스 관리자 ID
                    val nonAdminIds = memberIds.drop(1) // 관리자 제외한 나머지 멤버

                    if (nonAdminIds.isEmpty()) {
                        Toast.makeText(this, "관리자를 제외하면 팀원이 없습니다.", Toast.LENGTH_SHORT).show()
                        return@addOnSuccessListener
                    }

                    // Firestore에서 멤버 정보 가져오기
                    firestore.collection("Users")
                        .whereIn(FieldPath.documentId(), nonAdminIds)
                        .get()
                        .addOnSuccessListener { documents ->
                            val members = documents.map { doc ->
                                val name = doc.getString("name") ?: "이름 없음"
                                val profilePhoto = doc.getString("profilePhoto") ?: ""
                                Member(name, profilePhoto) // Member 객체 생성
                            }

                            // 어댑터에 데이터 설정
                            memberTeamListAdapter = MemberTeamListAdapter(members)
                            teamMemberList.adapter = memberTeamListAdapter

                            Log.d("MemberTeamList", "Loaded non-admin members: $members")
                        }
                        .addOnFailureListener { e ->
                            Log.e("MemberTeamList", "Failed to load members: ${e.message}")
                            Toast.makeText(this, "팀원 정보를 불러오지 못했습니다.", Toast.LENGTH_SHORT).show()
                        }
                } else {
                    Toast.makeText(this, "팀 정보를 찾을 수 없습니다.", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Log.e("AdminTeamList", "Failed to load team: ${e.message}")
                Toast.makeText(this, "팀 정보를 불러오지 못했습니다.", Toast.LENGTH_SHORT).show()
            }
    }

}

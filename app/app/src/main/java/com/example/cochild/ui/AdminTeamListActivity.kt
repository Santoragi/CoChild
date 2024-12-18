package com.example.cochild

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.cochild.models.Member
import com.example.cochild.AdminBaseActivity
import com.example.cochild.models.Team
import com.example.cochild.utils.TeamIdHelper
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldPath

class AdminTeamListActivity : AdminBaseActivity() {

    private lateinit var nowTeam: Team

    private lateinit var teamMemberList: RecyclerView
    private lateinit var adminTeamListAdapter: AdminTeamListAdapter

    private lateinit var requestList: RecyclerView
    private lateinit var adminRequestAdapter: AdminRequestAdapter

    private lateinit var firestore: FirebaseFirestore

    // 팀원 데이터를 관리할 MutableList
    private val teamMembers = mutableListOf<Member>() // Adapter와의 데이터 동기화를 위해 MutableList로 설정
    private val teamMemberIds = mutableListOf<String>() // UID도 관리

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_team_list)

        // Window Insets 설정
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.admin_team_list)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val teamId = TeamIdHelper.getTeamId(this)

        //하단바 설정
        if (teamId != null) {
            setupBottomNavigationView(R.id.nav_home, teamId)
        }
        //상단바
        setupToolbar()
        firestore = FirebaseFirestore.getInstance()

        // 팀 멤버 RecyclerView 설정
        teamMemberList = findViewById(R.id.team_member_list)
        teamMemberList.layoutManager = LinearLayoutManager(this)

        requestList = findViewById(R.id.request_list)
        requestList.layoutManager = LinearLayoutManager(this)


        if (teamId.isNullOrEmpty()) {
            Toast.makeText(this, "팀 ID가 없습니다.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        Log.d("AdminTeamList", "Received TEAM_ID: $teamId")

        // 어댑터 초기화
        setupAdapters(teamId)

        // 팀 멤버 및 가입 신청 리스트 로드
        loadTeamMembers(teamId)
        loadJoinRequests(teamId)
    }

    /**
     * 어댑터 초기화 함수
     */
    private fun setupAdapters(teamId: String) {
        // AdminTeamListAdapter 초기화
        adminTeamListAdapter = AdminTeamListAdapter(
            context = this,
            teamMembers = teamMembers,
            teamMemberIds = teamMemberIds,
            teamId = teamId,
            onMemberRemoved = { position ->
                // 멤버 삭제 후 UI 갱신 및 데이터 재로드
                Toast.makeText(this, "팀원이 제거되었습니다.", Toast.LENGTH_SHORT).show()
                loadTeamMembers(teamId)
            }
        )
        teamMemberList.adapter = adminTeamListAdapter
    }

    /**
     * 징식 팀원의 정보를 불러오는 함수
     */

    private fun loadTeamMembers(teamId: String) {
        firestore.collection("teams").document(teamId).get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val memberIds = document.get("members") as? List<String> ?: emptyList()

                    if (memberIds.isEmpty()) {
//                        Toast.makeText(this, "팀원이 없습니다.", Toast.LENGTH_SHORT).show()
                        return@addOnSuccessListener
                    }

                    // 관리자 ID를 추출하고 멤버 목록에서 제외
                    val adminId = memberIds.firstOrNull()
                    val nonAdminIds = memberIds.drop(1) // 관리자 제외한 멤버

                    if (nonAdminIds.isEmpty()) {
//                        Toast.makeText(this, "관리자를 제외하면 팀원이 없습니다.", Toast.LENGTH_SHORT).show()
                        return@addOnSuccessListener
                    }

                    // Firestore에서 멤버 정보 가져오기
                    firestore.collection("Users")
                        .whereIn(FieldPath.documentId(), nonAdminIds)
                        .get()
                        .addOnSuccessListener { documents ->
                            // HashMap으로 documents 변환
                            val documentMap = documents.associateBy { it.id }

                            // nonAdminIds 순서대로 데이터를 정렬하여 MutableList 갱신
                            teamMembers.clear()
                            teamMemberIds.clear()

                            nonAdminIds.forEach { id ->
                                documentMap[id]?.let { doc ->
                                    val name = doc.getString("name") ?: "이름 없음"
                                    val profilePhoto = doc.getString("profilePhoto") ?: ""
                                    teamMembers.add(Member(name, profilePhoto))
                                    teamMemberIds.add(id)
                                }
                            }

                            adminTeamListAdapter.notifyDataSetChanged()

                            Log.d("AdminTeamList", "Loaded non-admin members: $teamMembers")
                        }
                        .addOnFailureListener { e ->
                            Log.e("AdminTeamList", "Failed to load members: ${e.message}")
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


    /**
     * 가입 신청 리스트를 불러오는 함수
     */
    private fun loadJoinRequests(teamId: String) {
        firestore.collection("teams").document(teamId).get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val preMemberIds = document.get("preMembers") as? List<String> ?: emptyList()

                    if (preMemberIds.isEmpty()) {
//                        Toast.makeText(this, "가입 신청자가 없습니다.", Toast.LENGTH_SHORT).show()
                        adminRequestAdapter = AdminRequestAdapter(emptyList(), emptyList(), teamId) {
                            // 데이터 다시 로드
                            loadJoinRequests(teamId)
                        }
                        requestList.adapter = adminRequestAdapter
                        return@addOnSuccessListener
                    }

                    firestore.collection("Users")
                        .whereIn(FieldPath.documentId(), preMemberIds)
                        .get()
                        .addOnSuccessListener { documents ->
                            // HashMap으로 documents 변환
                            val documentMap = documents.associateBy { it.id }

                            // preMemberIds 순서대로 데이터를 정렬
                            val preMembers = preMemberIds.mapNotNull { id ->
                                documentMap[id]?.let { doc ->
                                    val name = doc.getString("name") ?: "이름 없음"
                                    val profilePhoto = doc.getString("profilePhoto") ?: ""
                                    Member(name, profilePhoto)
                                }
                            }

                            Log.d("AdminTeamList", "Loaded preMembers: $preMembers")

                            // 어댑터 업데이트
                            adminRequestAdapter = AdminRequestAdapter(preMembers, preMemberIds, teamId) {
                                // 데이터 다시 로드
                                loadJoinRequests(teamId)
                                loadTeamMembers(teamId)
                            }
                            requestList.adapter = adminRequestAdapter
                            adminRequestAdapter.notifyDataSetChanged()
                        }
                        .addOnFailureListener { e ->
                            Log.e("AdminTeamList", "Failed to load preMembers: ${e.message}")
                            Toast.makeText(this, "가입 신청 정보를 불러오지 못했습니다.", Toast.LENGTH_SHORT).show()
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

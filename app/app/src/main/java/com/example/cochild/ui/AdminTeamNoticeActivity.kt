package com.example.cochild

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.cochild.models.Team
import com.example.cochild.models.User
import com.example.cochild.utils.TeamIdHelper
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class AdminTeamNoticeActivity : AdminBaseActivity() {

    // 게시글 목록과 게시글들을 담아줄 어댑터
    private lateinit var TeamNoticeListAdapter: TeamNoticeListAdapter
    private lateinit var TeamNoticeList: RecyclerView

    private lateinit var team: Team
    private lateinit var teamId: String

    // 파이어베이스 연동
    private lateinit var firestore: FirebaseFirestore
    private lateinit var auth: FirebaseAuth


    private val noticeTitles = mutableListOf<String>()
    private val noticeContents = mutableListOf<String>()
    private val noticeCreatedTimes = mutableListOf<String>()
    private val noticeIds = mutableListOf<String>()
    private lateinit var  author : String

    private lateinit var currentUserId: String



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_admin_team_notice)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.admin_team_notice)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Firebase Firestore 인스턴스 초기화
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        //teamId = "6sYbSMOwX727g6w3Senb"
        currentUserId = auth.currentUser?.uid ?: ""
        teamId = TeamIdHelper.getTeamId(this).toString()


        //하단바 설정
        setupBottomNavigationView(R.id.nav_notice, teamId)
        //상단바
        setupToolbar()

        initializeData()

    }



    private fun initializeData() {
        val currentUserId = auth.currentUser?.uid ?: return

        // 팀 정보를 먼저 가져온 후 게시글 데이터 처리
        getTeam(teamId) { team ->
            if (team != null) {
                this.team = team


                // 게시글 작성 플로팅 버튼 추가
                val addNotice = findViewById<FloatingActionButton>(R.id.fab_create_notice)
                addNotice.setOnClickListener {
                    val intent = Intent(this, AdminCreateNoticeActivity::class.java)
                    intent.putExtra("TEAM_ID", team.teamId) // 팀 ID 전달
                    startActivity(intent)
                }


                // 게시글 데이터를 가져오기
                fetchNoticesFromTeam(teamId) { notices ->
                    // 배열 초기화


                    noticeTitles.clear()
                    noticeContents.clear()
                    noticeCreatedTimes.clear()
                    noticeIds.clear()

                    // 배열에 데이터 추가
                    notices.forEach { notice ->

                        noticeTitles.add(notice.title ?: "제목 없음")
                        noticeContents.add(notice.body ?: "")
                        noticeCreatedTimes.add(notice.createdTime)
                        noticeIds.add(notice.noticeId)



                    }

                    // 데이터를 어댑터에 전달
                    TeamNoticeListAdapter = TeamNoticeListAdapter(

                        noticeTitles,
                        noticeContents,
                        noticeCreatedTimes,
                        this.team,
                        currentUserId,
                        noticeIds,
                        author
                    )

                    // RecyclerView 설정
                    TeamNoticeList = findViewById(R.id.team_notice_list)
                    TeamNoticeList.layoutManager = LinearLayoutManager(this)
                    TeamNoticeList.adapter = TeamNoticeListAdapter
                    TeamNoticeListAdapter.notifyDataSetChanged() // 데이터 갱신
                }
            } else {
                Toast.makeText(this, "팀 데이터를 가져오지 못했습니다.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun getTeam(teamId: String, onComplete: (Team?) -> Unit) {
        firestore.collection("teams")
            .whereEqualTo("teamId", teamId)
            .get()
            .addOnSuccessListener { querySnapshot ->
                if (!querySnapshot.isEmpty) {

                    val team = querySnapshot.documents[0].toObject(Team::class.java)
                    onComplete(team) // 가져온 Team 객체를 콜백으로 전달

                } else {
                    onComplete(null) // 팀이 없을 경우 null 반환
                }
            }
            .addOnFailureListener { e ->
                onComplete(null) // 오류가 발생했을 경우 null 반환
            }
    }

    data class NoticeWithAuthor(
        val title: String?,
        val body: String?,
        val createdTime: String,
        val author: String,
        val noticeId: String
    )

    // 팀 ID로 공지사항 가져오기
    private fun fetchNoticesFromTeam(teamId: String, onComplete: (List<NoticeWithAuthor>) -> Unit) {
        firestore.collection("teams")
            .whereEqualTo("teamId", teamId)
            .get()
            .addOnSuccessListener { querySnapshot ->
                val notices = mutableListOf<NoticeWithAuthor>()

                for (document in querySnapshot.documents) {
                    val team = document.toObject(Team::class.java)

                    team?.notices?.forEach { notice ->
                        // post의 작성자 정보 가져오기
                        fetchUserById(notice.authorId) { user ->
                            val author = user?.name ?: "알 수 없음"
                            notices.add(
                                NoticeWithAuthor(
                                    notice.title,
                                    notice.body,
                                    notice.createdTime,
                                    author,
                                    notice.noticeId
                                )
                            )
                            // 모든 데이터가 완료되었을 경우 콜백 호출
                            if (notices.size == team.notices.size) {
                                onComplete(notices)
                            }
                        }
                    }

                }
            }
            .addOnFailureListener { e ->
                e.printStackTrace()
                Toast.makeText(this, "게시글 데이터를 가져오지 못했습니다.", Toast.LENGTH_SHORT).show()
            }
    }

    // userId로 유저 정보 가져오기
    private fun fetchUserById(userId: String, onComplete: (User?) -> Unit) {
        firestore.collection("Users")
            .document(userId)
            .get()
            .addOnSuccessListener { documentSnapshot ->
                val user = documentSnapshot.toObject(User::class.java)
                if (user != null) {
                    author = user.name

                }
                onComplete(user)
            }
            .addOnFailureListener { e ->
                e.printStackTrace()
                onComplete(null)
            }
    }

}


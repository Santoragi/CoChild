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

class AdminTeamBoardActivity : AdminBaseActivity() {

    // 게시글 목록과 게시글들을 담아줄 어댑터
    private lateinit var TeamPostListAdapter: TeamPostListAdapter
    private lateinit var TeamPostList: RecyclerView

    private lateinit var team: Team
    private lateinit var teamId: String

    // 파이어베이스 연동
    private lateinit var firestore: FirebaseFirestore
    private lateinit var auth: FirebaseAuth

    // 게시글 리스트
    private val postIds = mutableListOf<String>()
    private val postProfiles = mutableListOf<String>()
    private val postTitles = mutableListOf<String>()
    private val postContents = mutableListOf<String>()
    private val postAuthors = mutableListOf<String>() // 각 게시글의 작성자 정보를 저장

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_admin_team_board)

        // 시스템 바를 처리하여 엣지 투 엣지 모드 적용
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.admin_team_board)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Firebase Firestore 인스턴스 초기화
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        // 팀 ID 가져오기
        teamId = TeamIdHelper.getTeamId(this).toString()
        getTeam(teamId) { team ->
            if (team != null) {

                // 가져온 팀 데이터 처리
                this.team = team
                Log.d("getTeam", "가져온 팀: $team")

            } else {
                // 팀이 없거나 오류가 발생한 경우
                Toast.makeText(this, "팀을 찾을 수 없습니다.", Toast.LENGTH_SHORT).show()
            }
        }

        // 하단바 설정
        setupBottomNavigationView(R.id.nav_board, teamId)

        // 상단바 설정
        setupToolbar()



        // 게시글 데이터 초기화
        initializeData()
    }



    private fun initializeData() {
        val currentUserId = auth.currentUser?.uid ?: return

        // 팀 정보를 먼저 가져온 후 게시글 데이터 처리
        getTeam(teamId) { team ->
            if (team != null) {
                this.team = team

                // 게시글 작성 플로팅 버튼 추가
                val addPost = findViewById<FloatingActionButton>(R.id.fab_create_post)
                addPost.setOnClickListener {
                    val intent = Intent(this, AdminCreatePostActivity::class.java)
                    intent.putExtra("TEAM_ID", team.teamId) // 팀 ID 전달
                    startActivity(intent)
                }

                // 게시글 데이터를 가져오기
                fetchPostsFromTeam(teamId) { posts ->
                    // 배열 초기화
                    postIds.clear()
                    postAuthors.clear()
                    postProfiles.clear()
                    postTitles.clear()
                    postContents.clear()

                    // 배열에 데이터 추가
                    posts.forEach { post ->
                        postIds.add(post.postId ?:"")
                        postAuthors.add(post.author)
                        postProfiles.add(post.postPhoto ?: "")
                        postTitles.add(post.title ?: "제목 없음")
                        postContents.add(post.body ?: "")
                    }

                    // 데이터를 어댑터에 전달
                    TeamPostListAdapter = TeamPostListAdapter(
                        postIds,
                        postAuthors,
                        postProfiles,
                        postTitles,
                        postContents,
                        team,
                        currentUserId
                    )

                    // RecyclerView 설정
                    TeamPostList = findViewById(R.id.team_post_list)
                    TeamPostList.layoutManager = LinearLayoutManager(this)
                    TeamPostList.adapter = TeamPostListAdapter
                    TeamPostListAdapter.notifyDataSetChanged() // 데이터 갱신
                }
            } else {
                Toast.makeText(this, "팀 데이터를 가져오지 못했습니다.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // 게시글 데이터와 작성자 정보를 포함하는 데이터 모델
    data class PostWithAuthor(
        val postId: String?,
        val postPhoto: String?,
        val title: String?,
        val body: String?,
        val author: String
    )

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


    // 팀 ID로 게시글 가져오기
    private fun fetchPostsFromTeam(teamId: String, onComplete: (List<PostWithAuthor>) -> Unit) {
        firestore.collection("teams")
            .whereEqualTo("teamId", teamId)
            .get()
            .addOnSuccessListener { querySnapshot ->
                val posts = mutableListOf<PostWithAuthor>()

                for (document in querySnapshot.documents) {
                    val team = document.toObject(Team::class.java)

                    team?.posts?.forEach { post ->
                        // post의 작성자 정보 가져오기
                        fetchUserById(post.userId) { user ->
                            val authorName = user?.name ?: "알 수 없음"
                            posts.add(
                                PostWithAuthor(
                                    post.postId,
                                    post.postPhoto,
                                    post.title,
                                    post.body,
                                    authorName
                                )
                            )
                            // 모든 데이터가 완료되었을 경우 콜백 호출
                            if (posts.size == team.posts.size) {
                                onComplete(posts)
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
                onComplete(user)
            }
            .addOnFailureListener { e ->
                e.printStackTrace()
                onComplete(null)
            }
    }
}



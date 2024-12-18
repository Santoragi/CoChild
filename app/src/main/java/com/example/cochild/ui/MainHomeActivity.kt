package com.example.cochild

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.ImageButton
import android.widget.Toast
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.cochild.models.Post
import com.example.cochild.models.Team
import com.example.cochild.models.User
import com.google.android.gms.tasks.Task
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.toObjects

class MainHomeActivity : BaseActivity() {
    private lateinit var firestore: FirebaseFirestore
    private lateinit var auth: FirebaseAuth

    private lateinit var teamList: RecyclerView
    private lateinit var mainHomeTeamListAdapter: MainHomeTeamListAdapter

    private val teamData = mutableListOf<Team>()
    private val userTeamIds = mutableListOf<String>()

    private val allTeamData = mutableListOf<Team>()

    private lateinit var postList: RecyclerView
    private lateinit var postListAdapter: PostListAdapter

    private val postAuthors = mutableListOf<String>()
    private val postProfiles = mutableListOf<String>()
    private val postTitles = mutableListOf<String>()
    private val postContents = mutableListOf<String>()
    private val postTeams = mutableListOf<Team>()

    private val postData = mutableListOf<Post>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_home)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()



        // Window Insets 설정
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main_home)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        //상단바 설정
        setupToolbar()
        // 유저, 팀 데이터 불러오기


        teamList = findViewById(R.id.team_list)
        teamList.layoutManager = LinearLayoutManager(
            this,
            LinearLayoutManager.HORIZONTAL,
            false
        )

        loadUserData()

        val currentUserId = auth.currentUser?.uid ?: ""
        mainHomeTeamListAdapter = MainHomeTeamListAdapter(allTeamData, currentUserId)
        teamList.adapter = mainHomeTeamListAdapter

        postList = findViewById(R.id.post_list)
        postList.layoutManager = LinearLayoutManager(this)
        postListAdapter = PostListAdapter(postData, currentUserId)




        // Team add button
        findViewById<ImageButton>(R.id.team_add_button).setOnClickListener {
            startActivity(Intent(this, TeamCreateActivity::class.java))
        }
    }

//    private fun setupRecyclerViews() {
//        // Team list - horizontal
//        teamList = findViewById(R.id.team_list)
//        teamList.layoutManager = LinearLayoutManager(
//            this,
//            LinearLayoutManager.HORIZONTAL,
//            false
//        )
//
//        val currentUserId = auth.currentUser?.uid ?: ""
//        mainHomeTeamListAdapter = MainHomeTeamListAdapter(teamData, currentUserId)
//        teamList.adapter = mainHomeTeamListAdapter
//        mainHomeTeamListAdapter.notifyDataSetChanged()
//
//        Toast.makeText(this,"메인홈 팀 어댑터 설정", Toast.LENGTH_SHORT).show()
//
//        Log.d("Adapter", "teamData size: ${teamData.size}")
//
//
//
//        // Post list - vertical
//        postList = findViewById(R.id.post_list)
//        postList.layoutManager = LinearLayoutManager(this)
//
//
//    }

    private fun loadUserData() {
        val currentUserId = auth.currentUser?.uid ?: return


        firestore.collection("Users")
            .document(currentUserId)
            .get()
            .addOnSuccessListener { document ->
                val user = document.toObject(User::class.java)

                // Extract team IDs
                user?.teams?.forEach { teamMap ->
                    val teamId = teamMap["teamId"] as? String ?: return@forEach
                    userTeamIds.add(teamId)
                }

                // Load teams and posts after getting user's team IDs
                loadTeams {

                    mainHomeTeamListAdapter.notifyDataSetChanged()


                    // 팀 데이터 로드 완료 후 게시글 로드
                    loadPosts {
                        postList.adapter = postListAdapter
                        postListAdapter.notifyDataSetChanged()

                    }
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "사용자 데이터 로드 실패: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun loadTeams(onComplete: () -> Unit) {
        firestore.collection("teams")
            .get()
            .addOnSuccessListener { documents ->



                teamData.clear()
                allTeamData.clear()



                documents.forEach { document ->
                    val team = document.toObject(Team::class.java)

                    allTeamData.add(team)

                    if (userTeamIds.contains(team.teamId)) {
                        // Only add the teams that match userTeamIds
                        teamData.add(team)
                    }
                }

                // 콜백 호출
                onComplete()


            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "팀 데이터 로드 실패: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun loadPosts(onComplete: () -> Unit) {
        postData.clear() // Clear previous posts data

        // Extract posts from teamData
        teamData.forEach { team ->
            team.posts?.forEach { post ->
                postData.add(post) // Add posts to postData list
            }
        }
                // 콜백 호출
                onComplete()

    }


    override fun onResume() {
        super.onResume()
        loadUserData()
    }
}
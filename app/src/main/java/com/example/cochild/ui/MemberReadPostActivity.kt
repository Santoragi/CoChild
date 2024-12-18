package com.example.cochild

import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.cochild.models.Post
import com.example.cochild.models.Team
import com.example.cochild.models.User
import com.example.cochild.ui.MemberBaseActivity
import com.example.cochild.utils.TeamIdHelper.startActivityWithTeamId
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

class MemberReadPostActivity : MemberBaseActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private var postId: String = ""
    private var teamId: String = ""

    private var authorName: String = ""

    private lateinit var post: Post
    private var index: Int = 0
    private var previousPostId: String? = null
    private var nextPostId: String? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_member_read_post)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.member_read_post)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets

        }


        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        postId = intent.getStringExtra("POST_ID").toString()
        teamId = intent.getStringExtra("TEAM_ID").toString()


        //val teamId = TeamIdHelper.getTeamId(this).toString()
        //하단바 설정
        setupBottomNavigationView(R.id.nav_board, teamId)
        //상단바
        setupToolbar()


        // 배너 누르면 게시판 목록으로 이동
        val postBanner = findViewById<TextView>(R.id.post_banner)
        postBanner.setOnClickListener {
            startActivityWithTeamId(this, MemberTeamBoardActivity::class.java, teamId)
        }

        // 데이터 로드 및 동기화
        loadPostWithRetry(5, 3000)



    }

    private fun loadPostWithRetry(attempts: Int = 3, delayMillis: Long = 1000) {
        var remainingAttempts = attempts

        fun attemptLoadPost() {
            findPost(teamId, postId) { post ->
                if (post != null) {
                    this.post = post
                    fetchAuthorName(post.userId) { author ->

                        if (author != null) {

                            authorName = author
                            runOnUiThread {
                                updatePostUI(post) // 데이터를 가져온 후 UI 업데이트
                            }
                        }else {
                            runOnUiThread {
                                Toast.makeText(this, "작성자 정보를 가져오는데 실패했습니다.", Toast.LENGTH_SHORT).show()
                            }
                        }

                    }
                    getSortedPostsWithIndex(teamId)
                } else {
                    if (remainingAttempts > 0) {
                        remainingAttempts--
                        Log.d("PostLoad", "Retrying to load post. Attempts left: $remainingAttempts")
                        // 재시도 전 지연
                        thread {
                            Thread.sleep(delayMillis)
                            runOnUiThread {
                                attemptLoadPost()
                            }
                        }
                    } else {
                        runOnUiThread {
                            Toast.makeText(this, "게시물을 불러오는데 실패했습니다. 잠시 후 다시 시도해주세요.", Toast.LENGTH_LONG).show()
                            finish() // 액티비티 종료
                        }
                    }
                }
            }
        }

        attemptLoadPost()
    }


    private fun findPost(teamId: String, postId: String, callback: (Post?) -> Unit) {
        // Firestore에서 teams 컬렉션 안의 teamId 문서 -> posts 컬렉션 -> postId 필드를 가져오기
        firestore.collection("teams")
            .document(teamId) // teamId로 접근
            .get()
            .addOnSuccessListener { documentSnapshot ->
                // 문서가 존재하면 Post 객체로 변환
                val team = documentSnapshot.toObject(Team::class.java)
                val post = team?.posts?.find { it.postId == postId }

                // 결과를 콜백으로 전달
                callback(post)
            }
            .addOnFailureListener { e ->
                Log.e("findPost", "게시물 가져오기 실패", e)
                callback(null) // 실패 시 null 반환
            }
    }

    private fun updatePostUI(post: Post) {
        runOnUiThread {
            val postTitle = findViewById<TextView>(R.id.post_title)
            val postBody = findViewById<TextView>(R.id.post_body)
            val authorId = findViewById<TextView>(R.id.post_author)
            val createdDate = findViewById<TextView>(R.id.post_date)
            val postPhoto = findViewById<ImageView>(R.id.post_photo)

            postTitle.text = post.title
            postBody.text = post.body
            authorId.text = "작성자: ${this.authorName}"
            createdDate.text = "작성일시: 🕖${post.createdTime}"

            val postURI = post.postPhoto
            loadImageFromUrl(postURI, postPhoto)

            Toast.makeText(this, "게시물 정보를 불러왔습니다: ${post.title}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun getSortedPostsWithIndex(teamId: String) {
        firestore.collection("teams")
            .whereEqualTo("teamId", teamId)
            .get()
            .addOnSuccessListener { querySnapshot ->
                val posts = mutableListOf<Post>()
                for (document in querySnapshot.documents) {
                    val team = document.toObject(Team::class.java)
                    team?.posts?.forEach { post ->
                        posts.add(post)
                    }
                }

                val sortedPosts = posts.sortedBy { it.createdTime }

                Toast.makeText(this, "현재 게시물 : $postId", Toast.LENGTH_SHORT).show()
                val currentIndex = sortedPosts.indexOfFirst { it ->
                    it.postId.trim() == this.postId.trim().also {
                        Log.d("PostDebug", "PostId: ${it}")
                    }
                }
                Toast.makeText(this, "현재 인덱스: $currentIndex", Toast.LENGTH_SHORT).show()

                if (currentIndex != -1) {
                    // 현재 게시물 인덱스를 설정
                    index = currentIndex

                    // 이전 게시물 ID 설정
                    previousPostId = if (currentIndex > 0) sortedPosts[currentIndex - 1].postId else null
                    nextPostId = if (currentIndex < posts.size - 1) sortedPosts[currentIndex + 1].postId else null

                    // 이전 게시물 버튼 리스너 설정
                    val prevPost = findViewById<TextView>(R.id.prev_post)
                    prevPost.isEnabled = previousPostId != null
                    prevPost.setOnClickListener {
                        previousPostId?.let { id ->
                            navigateToPost(id)
                        } ?: Toast.makeText(this, "이전 게시물이 없습니다.", Toast.LENGTH_SHORT).show()
                    }

                    // 다음 게시물 버튼 리스너 설정
                    val nextPost = findViewById<TextView>(R.id.next_post)
                    nextPost.isEnabled = nextPostId != null
                    nextPost.setOnClickListener {
                        nextPostId?.let { id ->
                            navigateToPost(id)
                        } ?: Toast.makeText(this, "다음 게시물이 없습니다.", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Log.e("GetPostsWithIndex", "현재 postId($postId)에 해당하는 게시물을 찾을 수 없습니다.")
                }
            }
            .addOnFailureListener { e ->
                Log.e("GetPostsWithIndex", "게시물 목록을 가져오는데 실패했습니다.", e)
            }
    }

    private fun loadImageFromUrl(url: String?, imageView: ImageView) {
        if (url.isNullOrEmpty() || url.trim() == "null") {

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
                    Toast.makeText(this, "이미지 로드 실패", Toast.LENGTH_SHORT).show()
                }
                Log.e("TeamIntroActivity", "이미지 로드 실패", e)
            }
        }
    }

    // 게시물 상세 페이지로 이동
    private fun navigateToPost(postId: String) {
        val intent = Intent(this, MemberReadPostActivity::class.java)
        intent.putExtra("POST_ID", postId)
        intent.putExtra("TEAM_ID", teamId)
        startActivity(intent)
    }

    // userId로 유저 정보 가져오기
    private fun fetchAuthorName(userId: String, callback: (String?) -> Unit) {
        firestore.collection("Users")
            .document(userId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    // Firestore 문서를 User 객체로 변환
                    val user = document.toObject(User::class.java)
                    callback(user?.name) // name 필드 반환
                } else {
                    callback(null) // 문서가 없으면 null 반환
                }
            }
            .addOnFailureListener { e ->
                e.printStackTrace()
                callback(null) // 문서가 없으면 null 반환
            }
    }

}
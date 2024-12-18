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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.example.cochild.models.Notice
import com.example.cochild.models.Team
import com.example.cochild.models.User
import com.example.cochild.ui.MemberBaseActivity
import com.example.cochild.utils.TeamIdHelper.startActivityWithTeamId
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

class MemberReadNoticeActivity : MemberBaseActivity() {


    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private var noticeId: String = ""
    private var teamId: String = ""

    private var authorName: String = ""

    private lateinit var notice: Notice
    private var index: Int = 0
    private var previousNoticeId: String? = null
    private var nextNoticeId: String? = null



    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContentView(R.layout.activity_member_read_notice)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.member_read_notice)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets

        }

        // 1. firebase 연결 설정

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()


        noticeId = intent.getStringExtra("NOTICE_ID") ?: ""
        teamId = intent.getStringExtra("TEAM_ID") ?: ""
        //authorName = intent.getStringExtra("AUTHOR_NAME") ?: ""
        fetchUserById(noticeId) { author ->

            if (author != null) {
                authorName = author.name
            }

        }



        //하단바 설정
        setupBottomNavigationView(R.id.nav_notice,teamId)

        //상단바
        setupToolbar()



        // 공지사항 수정 , 삭제 버튼 활성화

        // 배너 누르면 공지사항 목록으로 이동
        val noticeBanner = findViewById<TextView>(R.id.notice_banner)
        noticeBanner.setOnClickListener{

            startActivityWithTeamId(this, MemberTeamNoticeActivity::class.java, teamId)
        }


        // 데이터 로드 및 동기화
        loadNoticeWithRetry(5, 3000)



    }

    private fun loadNoticeWithRetry(attempts: Int = 3, delayMillis: Long = 1000) {
        var remainingAttempts = attempts

        fun attemptLoadNotice() {
            findNotice(teamId, noticeId) { notice ->
                if (notice != null) {
                    this.notice = notice

                    fetchAuthorName(notice.authorId) { author ->

                        if (author != null) {

                            authorName = author
                            runOnUiThread {
                                //Toast.makeText(this, "공지사항 작성자: ${authorName}", Toast.LENGTH_SHORT).show()
                                updateNoticeUI(notice) // 데이터를 가져온 후 UI 업데이트
                            }
                        }else {
                            runOnUiThread {
                                Toast.makeText(this, "작성자 정보를 가져오는데 실패했습니다.", Toast.LENGTH_SHORT).show()
                            }
                        }

                    }

                    getSortedNoticesWithIndex(teamId)
                } else {
                    if (remainingAttempts > 0) {
                        remainingAttempts--
                        Log.d("NoticeLoad", "Retrying to load notice. Attempts left: $remainingAttempts")
                        // 재시도 전 지연
                        thread {
                            Thread.sleep(delayMillis)
                            runOnUiThread {
                                attemptLoadNotice()
                            }
                        }
                    } else {
                        runOnUiThread {
                            Toast.makeText(this, "공지사항을 불러오는데 실패했습니다. 잠시 후 다시 시도해주세요.", Toast.LENGTH_LONG).show()
                            finish() // 액티비티 종료
                        }
                    }
                }
            }
        }

        attemptLoadNotice()
    }


    private fun updateNoticeUI(notice: Notice) {
        runOnUiThread {
            val noticeTitle = findViewById<TextView>(R.id.notice_title)
            val noticeBody = findViewById<TextView>(R.id.notice_body)
            val authorName = findViewById<TextView>(R.id.notice_author)
            val createdDate = findViewById<TextView>(R.id.notice_date)
            val dueDate = findViewById<TextView>(R.id.notice_due_date)
            val noticePhoto = findViewById<ImageView>(R.id.notice_image)

            noticeTitle.text = notice.title
            noticeBody.text = notice.body
            authorName.text = "작성자: ${this.authorName}"
            createdDate.text = "작성일시: 🕖${notice.createdTime}"
            dueDate.text = "행사 예정일: 🕖${notice.dueDate}"

            val noticeURI = notice.noticePhoto
            loadImageFromUrl(noticeURI, noticePhoto)

            Toast.makeText(this, "공지사항 정보를 불러왔습니다: ${notice.title}", Toast.LENGTH_SHORT).show()
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


    // 공지사항 찾기
    private fun findNotice(teamId: String, noticeId: String, callback: (Notice?) -> Unit) {

        // Firestore에서 teams 컬렉션 안의 teamId 문서 -> notices 컬렉션 -> noticeId 필드를 가져오기
        firestore.collection("teams")
            .document(teamId) // teamId로 접근
            .get()
            .addOnSuccessListener { documentSnapshot ->

                // 문서가 존재하면 Notice 객체로 변환
                val team = documentSnapshot.toObject(Team::class.java)
                val notice = team?.notices?.find { it.noticeId == noticeId }

                // 결과를 콜백으로 전달
                callback(notice)
            }
            .addOnFailureListener { e ->
                Log.e("findNotice", "공지사항 가져오기 실패", e)
                callback(null) // 실패 시 null 반환
            }
    }


    private fun getSortedNoticesWithIndex(teamId: String) {
        firestore.collection("teams")
            .whereEqualTo("teamId", teamId)
            .get()
            .addOnSuccessListener { querySnapshot ->

                val notices = mutableListOf<Notice>()
                for (document in querySnapshot.documents) {

                    val team = document.toObject(Team::class.java)
                    team?.notices?.forEach { notice ->

                        notices.add(notice)
                    }
                }

                val sortedNotices = notices.sortedBy { it.dueDate }

                Toast.makeText(this, "현재 공지사항 : $noticeId", Toast.LENGTH_SHORT).show()
                val currentIndex = sortedNotices.indexOfFirst { it ->
                    it.noticeId.trim() == this.noticeId.trim().also {
                        Log.d("NoticeDebug", "NoticeId: ${it}")
                    }

                }
                Toast.makeText(this, "현재 인덱스: $currentIndex", Toast.LENGTH_SHORT).show()

                if (currentIndex != -1) {
                    // 현재 공지사항 인덱스를 설정
                    index = currentIndex

                    // 이전 공지사항 ID 설정
                    previousNoticeId = if (currentIndex > 0) sortedNotices[currentIndex - 1].noticeId else null
                    nextNoticeId = if (currentIndex < notices.size - 1) sortedNotices[currentIndex + 1].noticeId else null

                    // 이전 공지사항 버튼 리스너 설정
                    val prevNotice = findViewById<TextView>(R.id.prev_notice)
                    prevNotice.isEnabled = previousNoticeId != null
                    prevNotice.setOnClickListener {
                        previousNoticeId?.let { id ->
                            navigateToNotice(id)
                        } ?: Toast.makeText(this, "이전 공지사항이 없습니다.", Toast.LENGTH_SHORT).show()
                    }

                    // 다음 공지사항 버튼 리스너 설정
                    val nextNotice = findViewById<TextView>(R.id.next_notice)
                    nextNotice.isEnabled = nextNoticeId != null
                    nextNotice.setOnClickListener {
                        nextNoticeId?.let { id ->
                            navigateToNotice(id)
                        } ?: Toast.makeText(this, "다음 공지사항이 없습니다.", Toast.LENGTH_SHORT).show()
                    }

                } else {
                    Log.e("GetNoticesWithIndex", "현재 noticeId($noticeId)에 해당하는 공지사항을 찾을 수 없습니다.")
                }
            }
            .addOnFailureListener { e ->
                Log.e("GetNoticesWithIndex", "공지사항 목록을 가져오는데 실패했습니다.", e)
            }
    }

    // 공지사항 상세 페이지로 이동
    private fun navigateToNotice(noticeId: String) {
        val intent = Intent(this, MemberReadNoticeActivity::class.java)
        intent.putExtra("NOTICE_ID", noticeId)
        intent.putExtra("TEAM_ID", teamId)
        startActivity(intent)
    }

    // userId로 유저 정보 가져오기
    private fun fetchUserById(userId: String, onComplete: (User?) -> Unit) {
        firestore.collection("Users")
            .document(userId)
            .get()
            .addOnSuccessListener { documentSnapshot ->
                val user = documentSnapshot.toObject(User::class.java)
                if (user != null) {
                    Toast.makeText(this, "유저이름: ${user.name}", Toast.LENGTH_SHORT).show()
                }

                onComplete(user)
            }
            .addOnFailureListener { e ->
                e.printStackTrace()
                onComplete(null)
            }
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


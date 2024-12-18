package com.example.cochild


import android.content.Intent
import android.icu.text.SimpleDateFormat
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.CalendarView
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Calendar
import java.util.Locale
import com.example.cochild.models.Notice
import com.example.cochild.models.Team
import com.example.cochild.utils.TeamIdHelper


class AdminTeamCalendarActivity : AdminBaseActivity() {


    // 파베 인증과 파베 저장소 접근권한
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var teamId: String
    private var noticeId: String = "" // 공지사항 id
    private var selectedDate: String = ""  // 행사예정일 && 선택된 캘린더 날짜
    private var noticeList: List<Notice> = emptyList()


    override fun onCreate(savedInstanceState: Bundle?) {


        // -------------------기본 설정들----------------------
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_admin_team_calendar)


        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.admin_calendar)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }




        // ----------------DB 및 함수동작 설정-------------------


        // 1. firebase 연결 설정


        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()


        //val thisTeamId = "6sYbSMOwX727g6w3Senb"
        teamId = TeamIdHelper.getTeamId(this).toString()
        //teamId = thisTeamId



        //하단바 설정
        setupBottomNavigationView(R.id.nav_calendar, teamId)
        //상단바
        setupToolbar()



        // 2. 달력 관련 설정


        // 캘린더 객체
        val calendar = findViewById<CalendarView>(R.id.calendarView)


        // 캘린더의 선택된 날짜 텍스트 뷰
        val calendarDate = findViewById<TextView>(R.id.calendar_date)




        // SimpleDateFormat을 사용해 날짜를 원하는 형식으로 변환 및 초기날짜 세팅
        val calendarInstance01 = Calendar.getInstance()




        val dateFormat01 = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        selectedDate = dateFormat01.format(calendarInstance01.time)


        // 날짜 선택된 것을 확인하기 위한 코드
        Log.e("선택된 날짜 출력",selectedDate)


        calendarDate.text = selectedDate
        findNotices()


        // 이전 달을 추적하기 위한 변수
        var previousMonth = Calendar.getInstance().get(Calendar.MONTH)


        // 캘린더 날짜 선택 이벤트 리스너
        calendar.setOnDateChangeListener { _, year, month, dayOfMonth ->
            // SimpleDateFormat을 사용해 날짜를 원하는 형식으로 변환
            val calendarInstance = Calendar.getInstance()
            calendarInstance.set(year, month, dayOfMonth)


            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            selectedDate = dateFormat.format(calendarInstance.time)


            // 날짜 선택된 것을 확인하기 위한 코드
            Log.d("선택된 날짜 출력", selectedDate)
            calendarDate.text = selectedDate


            // 선택된 날짜로 공지사항 세팅
            todayNotice()
        }





    }


//    private fun highlightSelectedDate(notices: List<Notice>) {
//        val calendarView = findViewById<CalendarView>(R.id.calendarView)
//
//        // 공지사항의 dueDate 목록을 저장할 Set
//        val highlightedDates = mutableSetOf<Long>()
//
//        // notices 리스트에서 dueDate를 추출하여 Set에 추가
//        for (notice in notices) {
//            // dueDate가 null이 아닌 경우에만 처리
//            notice.dueDate.let { dueDate ->
//                // 날짜를 Calendar 객체로 변환하여 밀리초 단위로 저장
//                val calendar = Calendar.getInstance().apply {
//                    time = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(dueDate)
//                }
//                highlightedDates.add(calendar.timeInMillis) // Set에 날짜를 밀리초 단위로 저장
//            }
//        }
//
//        // CalendarView에서 날짜를 하이라이트
//        calendarView.setOnDateChangeListener { _, year, month, dayOfMonth ->
//            val selectedDate = Calendar.getInstance().apply {
//                set(year, month, dayOfMonth)
//            }
//
//            // 하이라이트된 날짜인지 확인하고, 날짜를 하이라이트
//            if (highlightedDates.contains(selectedDate.timeInMillis)) {
//                calendarView.setBackgroundColor(ContextCompat.getColor(this, R.color.highlighted_date))
//            } else {
//                calendarView.setBackgroundColor(ContextCompat.getColor(this, R.color.default_date))
//            }
//        }
//    }


    private fun findNotices() {
        val noticeContent = findViewById<TextView>(R.id.notice_content)


        firestore.collection("teams")
            .document(teamId) // teamId로 접근
            .get()
            .addOnSuccessListener { documentSnapshot ->
                // Firestore에서 가져온 문서를 출력
                Log.d("Team introduction", "팀 소개 문서들: $documentSnapshot")


                // 문서에서 notices 필드를 추출
                val notices = documentSnapshot.toObject(Team::class.java)?.notices ?: emptyList()
                Log.d("findNotices", "공지사항 목록: $notices")


                // 공지사항 리스트 출력
                if (notices.isNotEmpty()) {

                    noticeList = notices

                }


            }
            .addOnFailureListener { e ->
                Log.e("findNotices", "공지사항 가져오기 실패", e)
                // 실패 시 공지사항이 없다고 처리
                noticeContent.text = "공지사항을 가져오는데 실패했습니다."


            }
    }






    private fun todayNotice() {


        // 선택된 날짜와 일치하는 공지사항 필터링
        val filteredNotices = noticeList.filter {
            SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(
                SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(it.dueDate)
            ) == selectedDate
        }



        Toast.makeText(this, "$filteredNotices", Toast.LENGTH_SHORT).show()
        // 공지사항 버튼 설정 (필터링된 공지사항 전달)

        if(filteredNotices.isNotEmpty()) {

            goIntoNotice(filteredNotices)

        } else {
            createNotice(selectedDate)
        }


    }


    private fun goIntoNotice(notices: List<Notice>) {


        // 현재 출력해줄 공지사항의 인덱스
        var index = 0
        val noticesSize = notices.size

        // 공지사항 상세페이지로 이동하는 버튼
        val goIntoNoticeButtons = findViewById<LinearLayout>(R.id.notice_buttons_container)
        goIntoNoticeButtons.visibility = View.VISIBLE
        goIntoNoticeButtons.isEnabled = true



        // 공지사항 작성 비활성화
        val createNoticeButton = findViewById<Button>(R.id.create_notice_button)
        createNoticeButton.visibility = View.INVISIBLE
        createNoticeButton.isEnabled = false

        // 현재 공지사항 제목, 내용
        val noticeContent = findViewById<TextView>(R.id.notice_content)


        // 현재 공지사항 처리
        var currentNotice = notices[index]
        var noticeTitle = currentNotice.title
        val body = currentNotice.body

        var contentBuilder = StringBuilder()
        contentBuilder.append(" <${index + 1}번 공지: ${noticeTitle}>\n\n")
        contentBuilder.append("${body}\n")

        noticeContent.text = contentBuilder.toString()

        // 버튼 클릭 리스너 설정
        val goIntoNoticeButton = findViewById<Button>(R.id.go_into_notice_button)
        goIntoNoticeButton.setOnClickListener {

            // 공지사항 상세 페이지로 이동할 Intent 생성
            val intent = Intent(this, AdminReadNoticeActivity::class.java).apply {

                noticeId = currentNotice.noticeId


                // Intent에 공지사항 정보 전달
                putExtra("NOTICE_ID", noticeId)
                putExtra("TEAM_ID", teamId)


            }
            // 해당 Intent를 통해 AdminReadNotice로 화면 전환
            startActivity(intent)
        }

        // 공지사항 앞, 뒤 이동
        val previousButton = findViewById<ImageButton>(R.id.previous_notice)
        previousButton.setOnClickListener {


            if(index != 0)
            {
                index -=1
                //goIntoNoticeButton.text = "${index+1}번 공지사항 보기"

                // 현재 공지사항 처리
                currentNotice = notices[index]
                noticeTitle = currentNotice.title
                noticeContent.text = currentNotice.body

                contentBuilder = StringBuilder()
                contentBuilder.append(" <${index + 1}번 공지: ${currentNotice.title}>\n")
                contentBuilder.append("${currentNotice.body}\n")

                noticeContent.text = contentBuilder.toString()

            }
        }


        val nextButton = findViewById<ImageButton>(R.id.next_notice)
        nextButton.setOnClickListener {
            if(index != noticesSize-1)
            {
                index +=1
                //goIntoNoticeButton.text = "${index+1}번 공지사항 보기"

                // 현재 공지사항 처리
                currentNotice = notices[index]
                noticeTitle = currentNotice.title
                noticeContent.text = currentNotice.body

                contentBuilder = StringBuilder()
                contentBuilder.append(" <${index + 1}번 공지: ${currentNotice.title}>\n")
                contentBuilder.append("${currentNotice.body}\n")

                noticeContent.text = contentBuilder.toString()

            }
        }
    }


    private fun createNotice(dueDate: String) {

        // 공지사항 작성 활성화
        val createNoticeButton = findViewById<Button>(R.id.create_notice_button)

        // 공지사항 보기 비활성화
        val goIntoNoticeButtons = findViewById<LinearLayout>(R.id.notice_buttons_container)

        goIntoNoticeButtons.visibility = View.INVISIBLE
        goIntoNoticeButtons.isEnabled = false
        
        val noticeContent = findViewById<TextView>(R.id.notice_content)
        noticeContent.text = "공지사항 없음"

        createNoticeButton.text = "공지사항 작성하기"
        createNoticeButton.visibility = View.VISIBLE
        createNoticeButton.isEnabled = true

        createNoticeButton.setOnClickListener {

            // 공지사항 상세 페이지로 이동할 Intent 생성
            val intent = Intent(this, AdminCreateNoticeActivity::class.java).apply {


                // Intent에 공지사항 정보 전달
                putExtra("DUE_DATE", dueDate)
                putExtra("NOTICE_ID", noticeId)
                putExtra("TEAM_ID", teamId)


            }
            // 해당 Intent를 통해 AdminReadNotice로 화면 전환
            startActivity(intent)
        }

    }

}

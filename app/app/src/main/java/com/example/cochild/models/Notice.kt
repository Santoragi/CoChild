package com.example.cochild.models


data class Notice(
    val noticeId: String = "",         // 공지사항 ID
    val title: String = "",            // 공지 제목
    val body: String = "",             // 공지 내용
    val noticePhoto: String = "",      // 공지사항 이미지
    val createdTime: String = "",      // 생성 날짜, 시간
    val dueDate: String = "",          // 행사 예정일
    val authorId: String = "",         // 작성자 ID
    val teamId: String = ""            // 팀 ID
)

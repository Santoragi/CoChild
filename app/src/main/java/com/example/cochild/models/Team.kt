package com.example.cochild.models

data class Team(
    val teamId: String = "",
    val name: String = "",
    val category: String? = null,
    val profileImage: String = "",
    val introduction: String = "",
    val admin: String = "", // 팀 생성자의 UID
    val posts: List<Post> = emptyList(), // 팀 게시글 리스트
    val notices: List<Notice> = emptyList(), // 공지사항 리스트
    val members: List<String> = emptyList(), // 팀 멤버 UID 리스트
    val preMembers: List<String> = emptyList() // 가입 신청자 UID 리스트
)

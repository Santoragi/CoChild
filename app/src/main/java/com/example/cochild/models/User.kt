package com.example.cochild.models

data class User(
    val profilePhoto: String = "",
    val name: String = "",
    val gender: String = "",
    val birthday: String = "",
    val phone: String = "",
    val email: String = "",
    val teams: List<Map<String, String>> = emptyList()  // 사용자가 속한 팀 ID를 저장하는 필드
)

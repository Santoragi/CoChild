package com.example.cochild.utils

import android.content.Context
import android.util.Log
import android.widget.Toast
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage


object TeamUtils {

    /**
     * 팀장이 팀을 삭제하는 함수입니다.
     */
    fun deleteTeam(context: Context, teamId: String, profileImageUrl: String?, onSuccess: () -> Unit, onFailure: (String) -> Unit) {
        val firestore = FirebaseFirestore.getInstance()
        val storage = FirebaseStorage.getInstance()

        // 1. Storage에서 프로필 이미지 삭제
        if (!profileImageUrl.isNullOrEmpty()) {
            storage.getReferenceFromUrl(profileImageUrl).delete()
                .addOnSuccessListener {
                    Log.d("TeamDeleteUtils", "팀 이미지 삭제 성공: $profileImageUrl")
                }
                .addOnFailureListener { e ->
                    Log.e("TeamDeleteUtils", "팀 이미지 삭제 실패: ${e.message}")
                    onFailure("이미지 삭제 중 문제가 발생했습니다: ${e.message}")
                    return@addOnFailureListener
                }
        }

        // 2. Firestore에서 팀 문서 삭제
        firestore.collection("teams").document(teamId).delete()
            .addOnSuccessListener {
                Log.d("TeamDeleteUtils", "팀 삭제 성공: $teamId")
                Toast.makeText(context, "팀이 성공적으로 삭제되었습니다.", Toast.LENGTH_SHORT).show()
                onSuccess()
            }
            .addOnFailureListener { e ->
                Log.e("TeamDeleteUtils", "팀 삭제 실패: ${e.message}")
                onFailure("팀 삭제 중 문제가 발생했습니다: ${e.message}")
            }
    }

    /**
     * 팀장이 팀원 탈퇴시키는 함수입니다.
     */
    fun removeTeamMember(
        context: Context,
        teamId: String,
        memberId: String,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val firestore = FirebaseFirestore.getInstance()

        // Firestore 업데이트
        firestore.collection("teams").document(teamId)
            .update("members", FieldValue.arrayRemove(memberId))
            .addOnSuccessListener {
                Log.d("TeamUtils", "팀원 제거 성공: $memberId")
                Toast.makeText(context, "팀원 탈퇴 성공", Toast.LENGTH_SHORT).show()
                onSuccess()
            }
            .addOnFailureListener { exception ->
                Log.e("TeamUtils", "팀원 제거 실패: ${exception.message}")
                Toast.makeText(context, "팀원 탈퇴 실패: ${exception.message}", Toast.LENGTH_SHORT).show()
                onFailure(exception)
            }
    }
}

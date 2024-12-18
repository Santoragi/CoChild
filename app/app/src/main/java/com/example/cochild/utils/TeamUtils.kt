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
    fun deleteTeam(
        context: Context,
        teamId: String,
        profileImageUrl: String?,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
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

        // 2. Users 컬렉션의 teams 필드에서 해당 팀 정보 삭제
        firestore.collection("Users")
            .whereArrayContains("teams", mapOf("teamId" to teamId)) // 팀 ID로 필터링
            .get()
            .addOnSuccessListener { documents ->
                val batch = firestore.batch()

                for (document in documents) {
                    val userRef = firestore.collection("Users").document(document.id)
                    val teams = (document.get("teams") as? List<Map<String, String>>)
                        ?.filterNot { it["teamId"] == teamId } // 해당 팀 정보 제거

                    if (teams != null) {
                        batch.update(userRef, "teams", teams) // 업데이트
                    }
                }

                batch.commit()
                    .addOnSuccessListener {
                        Log.d("TeamDeleteUtils", "Users 컬렉션에서 팀 정보 삭제 성공")

                        // 3. Firestore에서 팀 문서 삭제
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
                    .addOnFailureListener { e ->
                        Log.e("TeamDeleteUtils", "Users 컬렉션 업데이트 실패: ${e.message}")
                        onFailure("Users 컬렉션 업데이트 중 문제가 발생했습니다: ${e.message}")
                    }
            }
            .addOnFailureListener { e ->
                Log.e("TeamDeleteUtils", "Users 컬렉션 조회 실패: ${e.message}")
                onFailure("Users 컬렉션 조회 중 문제가 발생했습니다: ${e.message}")
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

        // 1. Firestore에서 팀의 members 배열에서 해당 멤버 ID 삭제
        firestore.collection("teams").document(teamId)
            .update("members", FieldValue.arrayRemove(memberId))
            .addOnSuccessListener {
                Log.d("TeamUtils", "팀원 제거 성공: $memberId")

                // 2. Users 컬렉션에서 해당 멤버의 teams 배열에서 팀 정보 삭제
                firestore.collection("Users").document(memberId)
                    .get()
                    .addOnSuccessListener { document ->
                        if (document != null && document.exists()) {
                            val teams = (document.get("teams") as? List<Map<String, String>>)
                                ?.filterNot { it["teamId"] == teamId } // teamId에 해당하는 데이터 제거

                            if (teams != null) {
                                firestore.collection("Users").document(memberId)
                                    .update("teams", teams)
                                    .addOnSuccessListener {
                                        Log.d("TeamUtils", "Users 컬렉션에서 팀 정보 제거 성공")
                                        Toast.makeText(context, "팀 탈퇴 성공", Toast.LENGTH_SHORT).show()
                                        onSuccess()
                                    }
                                    .addOnFailureListener { e ->
                                        Log.e("TeamUtils", "Users 컬렉션 업데이트 실패: ${e.message}")
                                        Toast.makeText(context, "사용자 정보 업데이트 실패: ${e.message}", Toast.LENGTH_SHORT).show()
                                        onFailure(e)
                                    }
                            } else {
                                Log.e("TeamUtils", "Users 컬렉션에서 teams 데이터가 비어 있음")
                                Toast.makeText(context, "사용자 정보가 비어 있습니다.", Toast.LENGTH_SHORT).show()
                                onSuccess() // teams 필드가 비어 있어도 탈퇴 처리 성공으로 간주
                            }
                        } else {
                            Log.e("TeamUtils", "Users 컬렉션에서 해당 사용자 문서를 찾을 수 없음")
                            Toast.makeText(context, "사용자 정보를 찾을 수 없습니다.", Toast.LENGTH_SHORT).show()
                            onSuccess() // 사용자 문서가 없어도 탈퇴 처리 성공으로 간주
                        }
                    }
                    .addOnFailureListener { e ->
                        Log.e("TeamUtils", "Users 컬렉션 조회 실패: ${e.message}")
                        Toast.makeText(context, "사용자 정보 조회 실패: ${e.message}", Toast.LENGTH_SHORT).show()
                        onFailure(e)
                    }
            }
            .addOnFailureListener { exception ->
                Log.e("TeamUtils", "팀원 제거 실패: ${exception.message}")
                Toast.makeText(context, "팀원 탈퇴 실패: ${exception.message}", Toast.LENGTH_SHORT).show()
                onFailure(exception)
            }
    }
}

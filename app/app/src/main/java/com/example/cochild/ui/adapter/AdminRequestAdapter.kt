package com.example.cochild

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.example.cochild.models.Member
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread
import android.graphics.BitmapFactory
import android.util.Log
import com.example.cochild.models.Team
import com.example.cochild.models.User

class AdminRequestAdapter(
    private val preMembers: List<Member>, // 가입 신청자 정보
    private val preMemberIds: List<String>, // 가입 신청자 Firestore 문서 ID
    private val teamId: String, // 현재 팀 ID
    private val onActionCompleted: () -> Unit // 콜백 인터페이스 추가
) : RecyclerView.Adapter<AdminRequestAdapter.RequestViewHolder>() {

    inner class RequestViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val profileImage: ImageView = itemView.findViewById(R.id.request_profile)
        val requestName: TextView = itemView.findViewById(R.id.request_name)
        val acceptButton: Button = itemView.findViewById(R.id.accept_button)
        val refuseButton: Button = itemView.findViewById(R.id.refuse_button)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RequestViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.request_list_horizontal, parent, false)
        return RequestViewHolder(view)
    }

    override fun onBindViewHolder(holder: RequestViewHolder, position: Int) {
        val member = preMembers[position]
        val userId = preMemberIds[position] // 해당 사용자의 Firestore 문서 ID

        // 사용자 이름과 프로필 이미지 설정
        holder.requestName.text = member.name
        if (member.profilePhoto.isNotEmpty()) {
            // 프로필 이미지 로드
            loadImageFromUrl(member.profilePhoto, holder.profileImage)
        } else {
            holder.profileImage.setImageResource(R.drawable.ic_launcher_foreground) // 기본 이미지
        }

        // 수락 버튼 클릭 리스너
        holder.acceptButton.setOnClickListener {
            FirebaseFirestore.getInstance().collection("teams").document(teamId)
                .update(
                    "preMembers", FieldValue.arrayRemove(userId), // 가입 신청 리스트에서 제거
                    "members", FieldValue.arrayUnion(userId) // 멤버 리스트에 추가
                )
                .addOnSuccessListener {
                    // Users 컬렉션에서 사용자 객체 가져오기
                    getUser(userId) { user ->
                        if (user != null) {
                            // 팀 정보를 추가

                            getTeam(teamId) { teamName ->
                                val teamInfo = mapOf("name" to teamName, "teamId" to teamId)

                                // 사용자의 teams 배열 업데이트
                                FirebaseFirestore.getInstance().collection("Users").document(userId)
                                    .update("teams", FieldValue.arrayUnion(teamInfo))
                                    .addOnSuccessListener {
                                        Toast.makeText( holder.itemView.context,"가입 신청을 수락했습니다.", Toast.LENGTH_SHORT ).show()
                                        Log.d("RequestAdapter","User $userId added to team $teamId with name $teamName" )
                                        onActionCompleted() // 콜백 호출
                                    }
                                    .addOnFailureListener { e ->
                                        Toast.makeText( holder.itemView.context,"사용자 팀 정보 업데이트에 실패했습니다: ${e.message}", Toast.LENGTH_SHORT ).show()
                                    }
                            }
                        } else {
                            Toast.makeText(holder.itemView.context,"사용자 데이터를 찾을 수 없습니다.", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText( holder.itemView.context,"가입 신청 수락에 실패했습니다: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }

        // 거절 버튼 클릭 리스너
        holder.refuseButton.setOnClickListener {
            FirebaseFirestore.getInstance().collection("teams").document(teamId)
                .update("preMembers", FieldValue.arrayRemove(userId)) // 가입 신청 리스트에서만 제거
                .addOnSuccessListener {
                    Toast.makeText( holder.itemView.context,"가입 신청을 거절했습니다.", Toast.LENGTH_SHORT).show()
                    Log.d("RequestAdapter", "User $userId refused and removed from preMembers") // 로그 추가
                    onActionCompleted() // 콜백 호출
                }
                .addOnFailureListener { e ->
                    Toast.makeText( holder.itemView.context,"가입 신청 거절에 실패했습니다: ${e.message}",Toast.LENGTH_SHORT).show()
                }
        }
    }

    override fun getItemCount(): Int = preMembers.size

    private fun loadImageFromUrl(url: String, imageView: ImageView) {
        thread {
            try {
                val connection = URL(url).openConnection() as HttpURLConnection
                connection.doInput = true
                connection.connect()
                val inputStream = connection.inputStream
                val bitmap = BitmapFactory.decodeStream(inputStream)

                imageView.post {
                    imageView.setImageBitmap(bitmap)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun getUser(userId: String, callback: (User?) -> Unit) {
        FirebaseFirestore.getInstance().collection("Users").document(userId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val user = document.toObject(User::class.java)
                    callback(user) // 성공적으로 가져온 사용자 객체 반환
                } else {
                    callback(null) // 문서가 없으면 null 반환
                }
            }
            .addOnFailureListener {
                callback(null) // 오류 발생 시 null 반환
            }
    }

    private fun getTeam(teamId: String, callback: (String?) -> Unit) {

        FirebaseFirestore.getInstance().collection("teams").document(teamId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val team = document.toObject(Team::class.java)
                    if (team != null) {
                        callback(team.name)
                    } // 성공적으로 가져온 사용자 객체 반환
                } else {
                    callback(null) // 문서가 없으면 null 반환
                }
            }
            .addOnFailureListener {
                callback(null) // 오류 발생 시 null 반환
            }


    }
}

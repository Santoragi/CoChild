package com.example.cochild

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.cochild.models.Member
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread
import android.graphics.BitmapFactory
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.example.cochild.utils.TeamUtils

class AdminTeamListAdapter(
//    private val members: List<Member>
    private val context: Context,
    private val teamMembers: MutableList<Member>, // 팀원 리스트 (변경 가능)
    private val teamMemberIds: MutableList<String>, // 팀원의 UID 리스트
    private val teamId: String, // 팀 ID
    private val onMemberRemoved: (Int) -> Unit // 멤버 삭제 후 콜백 (position 전달)
) : RecyclerView.Adapter<AdminTeamListAdapter.TeamMemberViewHolder>() {

    class TeamMemberViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val profileImage: ImageView = itemView.findViewById(R.id.member_profile)
        val memberName: TextView = itemView.findViewById(R.id.member_name)
        val outButton: Button = itemView.findViewById(R.id.out_button)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TeamMemberViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.member_list_horizontal, parent, false)
        return TeamMemberViewHolder(view)
    }

    override fun onBindViewHolder(holder: TeamMemberViewHolder, position: Int) {
        val member = teamMembers[position]
        val memberId = teamMemberIds[position]

        // 프로필 이미지 로드
        holder.memberName.text = member.name
        if (member.profilePhoto.isNotEmpty()) {
            loadImageFromUrl(member.profilePhoto, holder.profileImage)
        } else {
            holder.profileImage.setImageResource(R.drawable.ic_launcher_foreground) // 기본 이미지
        }

        // 탈퇴 버튼 클릭 리스너
        holder.outButton.setOnClickListener {
            showConfirmationDialog(member, memberId, position)
        }
    }

    override fun getItemCount(): Int = teamMembers.size

    private fun showConfirmationDialog(member: Member, memberId: String, position: Int) {
        AlertDialog.Builder(context)
            .setTitle("팀원 탈퇴")
            .setMessage("${member.name}님을 팀에서 탈퇴시키겠습니까?")
            .setPositiveButton("탈퇴") { _, _ ->
                removeTeamMember(member, memberId, position)
            }
            .setNegativeButton("취소", null)
            .show()
    }

    private fun removeTeamMember(member: Member, memberId: String, position: Int) {
        TeamUtils.removeTeamMember(
            context = context,
            teamId = teamId,
            memberId = memberId,
            onSuccess = {
                // 성공 시 RecyclerView 업데이트
                teamMembers.removeAt(position)
                teamMemberIds.removeAt(position)
                notifyItemRemoved(position)
                notifyItemRangeChanged(position, teamMembers.size)
                onMemberRemoved(position)
//                Toast.makeText(context, "${member.name}님을 성공적으로 탈퇴시켰습니다.", Toast.LENGTH_SHORT).show()
            },
            onFailure = { exception ->
                // 실패 시 오류 메시지 표시
                Toast.makeText(context, "탈퇴 실패: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
        )
    }

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
}

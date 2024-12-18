package com.example.cochild

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.cochild.models.Team
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread
import android.graphics.BitmapFactory
import android.widget.Toast

/*
class TeamListAdapter(
    private val teamProfiles: List<Int>,
    private val teamNames: List<String>
) : RecyclerView.Adapter<TeamListAdapter.TeamViewHolder>() {

    inner class TeamViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val profileImage: ImageView = itemView.findViewById(R.id.team_profile)
        val teamName: TextView = itemView.findViewById(R.id.team_name)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TeamViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.team_list, parent, false)
        return TeamViewHolder(view)
    }

    override fun onBindViewHolder(holder: TeamViewHolder, position: Int) {
        holder.profileImage.setImageResource(teamProfiles[position])
        holder.teamName.text = teamNames[position]
    }

    override fun getItemCount(): Int = teamProfiles.size
}
 */

class MainHomeTeamListAdapter(
    private val teamList: List<Team>, // 팀 데이터를 리스트로 받음
    private val currentUserId: String // 현재 로그인한 사용자의 UID
) : RecyclerView.Adapter<MainHomeTeamListAdapter.TeamViewHolder>() {

    inner class TeamViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val profileImage: ImageView = itemView.findViewById(R.id.team_profile)
        val teamName: TextView = itemView.findViewById(R.id.team_name)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TeamViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.team_list, parent, false)
        return TeamViewHolder(view)
    }

    override fun onBindViewHolder(holder: TeamViewHolder, position: Int) {
        val team = teamList[position]

        // 팀 이름 설정
        holder.teamName.text = team.name

        // 팀 프로필 이미지 로드
        if (!team.profileImage.isNullOrEmpty()) {
            loadImageFromUrl(team.profileImage, holder.profileImage)
        } else {
            holder.profileImage.setImageResource(R.drawable.groups_100dp_5f6368) // 기본 이미지
        }

        // 이미지 클릭 이벤트
        holder.profileImage.setOnClickListener {
            handleTeamProfileClick(holder, team)
        }
    }

    override fun getItemCount(): Int = teamList.size

    /**
     * 팀 프로필 이미지 클릭 이벤트 처리 함수
     */
    private fun handleTeamProfileClick(holder: TeamViewHolder, team: Team) {
        when {
            team.admin == currentUserId -> {
                // 팀장인 경우 AdminTeamHomeActivity로 이동
                val intent = Intent(holder.itemView.context, AdminTeamHomeActivity::class.java)
                intent.putExtra("TEAM_ID", team.teamId) // 팀 ID 전달
                holder.itemView.context.startActivity(intent)

                // 토스트 메시지 표시
                Toast.makeText(
                    holder.itemView.context,
                    "팀장 입니다.",
                    Toast.LENGTH_SHORT
                ).show()
            }
            team.members.contains(currentUserId) -> {
                // 멤버인 경우 MemberTeamHome으로 이동
                val intent = Intent(holder.itemView.context, MemberTeamHomeActivity::class.java)
                intent.putExtra("TEAM_ID", team.teamId) // 팀 ID 전달
                holder.itemView.context.startActivity(intent)

                // 토스트 메시지 표시
                Toast.makeText(
                    holder.itemView.context,
                    "멤버 입니다.",
                    Toast.LENGTH_SHORT
                ).show()
            }
            else -> {
                // 팀장이 아니고 멤버도 아닌 경우 TeamIntroActivity로 이동
                val intent = Intent(holder.itemView.context, TeamIntroActivity::class.java)
                intent.putExtra("TEAM_ID", team.teamId) // 팀 ID 전달
                holder.itemView.context.startActivity(intent)

                // 토스트 메시지 표시
                Toast.makeText(
                    holder.itemView.context,
                    "팀장이 아니며 멤버로 등록되어 있지 않습니다.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    /**
     * URL에서 이미지를 로드하는 함수
     */
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

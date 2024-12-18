package com.example.cochild

import android.content.Intent
import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.example.cochild.MainHomeTeamListAdapter.TeamViewHolder
import com.example.cochild.models.Team
import com.example.cochild.models.User
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

class TeamPostListAdapter(

    private val postIds: List<String>,
    private val postAuthors: List<String>,
    private val postProfiles: List<String>,
    private val postTitles: List<String>,
    private val postContents: List<String>,
    private val team: Team,
    private val currentUserId: String // 현재 로그인한 사용자의 UID

) : RecyclerView.Adapter<TeamPostListAdapter.TeamPostViewHolder>() {

    inner class TeamPostViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        val userName: TextView =  itemView.findViewById(R.id.user_name)
        val profileImage: ImageView = itemView.findViewById(R.id.user_profile)
        val postTitle: TextView = itemView.findViewById(R.id.head)
        val postContent: TextView = itemView.findViewById(R.id.body)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TeamPostViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.post_list, parent, false)
        return TeamPostViewHolder(view)
    }

    override fun onBindViewHolder(holder: TeamPostViewHolder, position: Int) {

        val imageUrl = postProfiles[position]

        // 프로필 이미지 로드
        if (imageUrl.isNotEmpty()) {
            loadImageFromUrl(imageUrl, holder.profileImage)
        } else {
            holder.profileImage.setImageResource(R.drawable.ic_launcher_foreground) // 기본 이미지
        }
        val postId = postIds[position]
        holder.userName.text = postAuthors[position]
        holder.postTitle.text = postTitles[position]
        holder.postContent.text = postContents[position]

        holder.postContent.setOnClickListener {
            handleTeamProfileClick(holder, team,postId)
        }
        holder.userName.setOnClickListener {
            handleTeamProfileClick(holder, team, postId)
        }
        holder.postTitle.setOnClickListener {
            handleTeamProfileClick(holder, team, postId)
        }
        holder.profileImage.setOnClickListener {
            handleTeamProfileClick(holder, team, postId)
        }

    }

    override fun getItemCount(): Int = postProfiles.size

    private fun handleTeamProfileClick(holder: TeamPostViewHolder, team: Team, postId: String) {
        when {

            team.admin == currentUserId -> {
                // 팀장인 경우 AdminTeamHomeActivity로 이동
                val intent = Intent(holder.itemView.context, AdminReadPostActivity::class.java)
                intent.putExtra("TEAM_ID", team.teamId) // 팀 ID 전달
                intent.putExtra("POST_ID", postId) // 게시글 ID 전달
                holder.itemView.context.startActivity(intent)

                // 토스트 메시지 표시
//                Toast.makeText(
//                    holder.itemView.context,
//                    "팀장 입니다.",
//                    Toast.LENGTH_SHORT
//                ).show()
            }
            team.members.contains(currentUserId) -> {
                // 멤버인 경우 MemberTeamHome으로 이동
                val intent = Intent(holder.itemView.context, MemberReadPostActivity::class.java)
                intent.putExtra("TEAM_ID", team.teamId) // 팀 ID 전달
                intent.putExtra("POST_ID", postId) // 게시글 ID 전달
                holder.itemView.context.startActivity(intent)

                // 토스트 메시지 표시
//                Toast.makeText(
//                    holder.itemView.context,
//                    "멤버 입니다.",
//                    Toast.LENGTH_SHORT
//                ).show()
            }
        }
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

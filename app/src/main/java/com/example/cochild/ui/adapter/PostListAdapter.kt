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
import com.example.cochild.models.Post
import com.example.cochild.models.Team
import com.example.cochild.models.User
import com.google.firebase.firestore.FirebaseFirestore
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

class PostListAdapter(


    private val posts: List<Post>,
    private val currentUserId: String // 현재 로그인한 사용자의 UID

) : RecyclerView.Adapter<PostListAdapter.PostViewHolder>() {

    private val firestore = FirebaseFirestore.getInstance()

    inner class PostViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        val userName: TextView =  itemView.findViewById(R.id.user_name)
        val profileImage: ImageView = itemView.findViewById(R.id.user_profile)
        val postTitle: TextView = itemView.findViewById(R.id.head)
        val postContent: TextView = itemView.findViewById(R.id.body)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.post_list, parent, false)
        return PostViewHolder(view)
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {

        val userId = posts[position].userId
        val imageUrl = posts[position].postPhoto

        // 프로필 이미지 로드
        if (imageUrl.isNotEmpty()) {
            loadImageFromUrl(imageUrl, holder.profileImage)
        } else {
            holder.profileImage.setImageResource(R.drawable.ic_launcher_foreground) // 기본 이미지
        }

        loadUserName(userId) { userName ->
            holder.userName.text = userName ?: "Unknown" // 이름 없을 경우 기본값
        }


        holder.postTitle.text = posts[position].title
        holder.postContent.text = posts[position].body


        loadTeam(posts[position].teamId) { team ->


                if (team != null) {
                    holder.postContent.setOnClickListener {
                        handleTeamProfileClick(holder, team)
                    }

                    holder.userName.setOnClickListener {
                        handleTeamProfileClick(holder, team)
                    }
                    holder.postTitle.setOnClickListener {
                        handleTeamProfileClick(holder, team)
                    }
                    holder.profileImage.setOnClickListener {
                        handleTeamProfileClick(holder, team)
                    }
                }



        }



    }

    override fun getItemCount(): Int = posts.size

    private fun handleTeamProfileClick(holder: PostViewHolder, team: Team) {
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
        }
    }

    private fun loadUserName(userId: String, callback: (String?) -> Unit) {
        firestore.collection("Users")
            .document(userId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    // Firestore 문서를 User 객체로 변환
                    val user = document.toObject(User::class.java)
                    callback(user?.name) // name 필드 반환
                } else {
                    callback(null) // 문서가 없으면 null 반환
                }
            }
            .addOnFailureListener {
                callback(null) // 오류 발생 시 null 반환
            }
    }


    private fun loadTeam(teamId: String, callback: (Team?) -> Unit) {
        firestore.collection("teams")
            .document(teamId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    // Firestore 문서를 User 객체로 변환
                    val team = document.toObject(Team::class.java)
                    callback(team) // name 필드 반환
                } else {
                    callback(null) // 문서가 없으면 null 반환
                }
            }
            .addOnFailureListener {
                callback(null) // 오류 발생 시 null 반환
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

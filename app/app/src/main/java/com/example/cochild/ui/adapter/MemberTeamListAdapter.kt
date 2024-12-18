package com.example.cochild.ui.adapter

import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.cochild.R
import com.example.cochild.models.Member
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

class MemberTeamListAdapter(
    private val members: List<Member>
) : RecyclerView.Adapter<MemberTeamListAdapter.TeamMemberViewHolder>() {

    class TeamMemberViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val profileImage: ImageView = itemView.findViewById(R.id.member_profile)
        val memberName: TextView = itemView.findViewById(R.id.member_name)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TeamMemberViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.member_horizontal, parent, false)
        return TeamMemberViewHolder(view)
    }

    override fun onBindViewHolder(holder: TeamMemberViewHolder, position: Int) {
        val member = members[position]
        holder.memberName.text = member.name

        // 프로필 이미지 로드
        if (member.profilePhoto.isNotEmpty()) {
            loadImageFromUrl(member.profilePhoto, holder.profileImage)
        } else {
            holder.profileImage.setImageResource(R.drawable.ic_launcher_foreground) // 기본 이미지
        }

    }

    override fun getItemCount(): Int = members.size

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

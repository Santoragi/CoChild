package com.example.cochild

import android.app.Activity
import android.content.Context
import android.graphics.BitmapFactory
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.cochild.models.Member
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

class AdminProfileAdapter(private val context: Context) :
    RecyclerView.Adapter<AdminProfileAdapter.MemberViewHolder>() {

    private val members = mutableListOf<Member>()

    fun updateMembers(newMembers: List<Member>) {
        members.clear()
        members.addAll(newMembers)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MemberViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_member, parent, false)
        return MemberViewHolder(view)
    }

    override fun onBindViewHolder(holder: MemberViewHolder, position: Int) {
        val member = members[position]
        holder.bind(member)
    }

    override fun getItemCount(): Int = members.size

    inner class MemberViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val nameView: TextView = itemView.findViewById(R.id.member_name)
        private val profileView: ImageView = itemView.findViewById(R.id.member_profile_image)

        fun bind(member: Member) {
            nameView.text = member.name
            loadImageFromUrl(member.profilePhoto, profileView)
        }

        private fun loadImageFromUrl(url: String?, imageView: ImageView) {
            if (url.isNullOrEmpty()) {
                imageView.setImageResource(R.drawable.ic_launcher_foreground)
                return
            }

            thread {
                try {
                    val connection = URL(url).openConnection() as HttpURLConnection
                    connection.doInput = true
                    connection.connect()
                    val inputStream = connection.inputStream
                    val bitmap = BitmapFactory.decodeStream(inputStream)

                    (context as Activity).runOnUiThread {
                        imageView.setImageBitmap(bitmap)
                    }
                } catch (e: Exception) {
                    Log.e("MemberProfileAdapter", "이미지 로드 실패: ${e.message}", e)
                    (context as Activity).runOnUiThread {
                        imageView.setImageResource(R.drawable.ic_launcher_foreground)
                    }
                }
            }
        }
    }
}




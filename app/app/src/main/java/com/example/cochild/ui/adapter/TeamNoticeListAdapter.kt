package com.example.cochild

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.example.cochild.models.Team

class TeamNoticeListAdapter(

    // 공지를 작성할 수 있는 팀장의 id

    private val noticeTitles: List<String>,
    private val noticeContents: List<String>,
    private val noticeCreatedTimes: List<String>,
    private val team: Team,
    private val currentUserId: String,
    private val noticeIds:  List<String>,
    private val authorName: String



) : RecyclerView.Adapter<TeamNoticeListAdapter.TeamNoticeViewHolder>() {

    inner class TeamNoticeViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        val noticeTitle: TextView = itemView.findViewById(R.id.notice_title)
        val noticeContent: TextView = itemView.findViewById(R.id.notice_body)
        val noticeCreatedTime: TextView = itemView.findViewById(R.id.notice_date)
        val authorName: TextView = itemView.findViewById(R.id.notice_author)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TeamNoticeViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.notice_list, parent, false)
        return TeamNoticeViewHolder(view)
    }

    override fun onBindViewHolder(holder: TeamNoticeViewHolder, position: Int) {


        holder.noticeTitle.text = noticeTitles[position]
        holder.noticeContent.text = noticeContents[position]
        holder.noticeCreatedTime.text = noticeCreatedTimes[position]
        holder.authorName.text = authorName

        val authorName = authorName
        val noticeId = noticeIds[position]
        holder.itemView.setOnClickListener {
            handleTeamProfileClick(holder, team, noticeId, authorName )
        }



    }

    override fun getItemCount(): Int = noticeIds.size

    private fun handleTeamProfileClick(holder: TeamNoticeViewHolder, team: Team, noticeId: String, authorName: String) {
        val context = holder.itemView.context
        val intent: Intent

        when {

            team.admin == currentUserId -> {
                // 팀장인 경우 AdminTeamHomeActivity로 이동
                intent = Intent(holder.itemView.context, AdminReadNoticeActivity::class.java)
                intent.putExtra("TEAM_ID", team.teamId) // 팀 ID 전달
                intent.putExtra("NOTICE_ID", noticeId) // 공지사항 ID 전달
                intent.putExtra("AUTHOR_NAME", authorName) // 작성자 이름 전달
//                Toast.makeText(holder.itemView.context,"공지사항 id : $noticeId", Toast.LENGTH_SHORT ).show()

                // 토스트 메시지 표시
//                Toast.makeText(
//                    holder.itemView.context,
//                    "팀장 입니다.",
//                    Toast.LENGTH_SHORT
//                ).show()
            }
            team.members.contains(currentUserId) -> {
                // 멤버인 경우 MemberTeamHome으로 이동
                intent = Intent(holder.itemView.context, MemberReadNoticeActivity::class.java)
                intent.putExtra("TEAM_ID", team.teamId) // 팀 ID 전달
                intent.putExtra("NOTICE_ID", noticeId) // 공지사항 ID 전달
                intent.putExtra("AUTHOR_NAME", authorName) // 작성자 이름 전달

                // 토스트 메시지 표시
//                Toast.makeText(
//                    holder.itemView.context,
//                    "멤버 입니다.",
//                    Toast.LENGTH_SHORT
//                ).show()
            }
            else -> return
        }
        context.startActivity(intent)
    }
}




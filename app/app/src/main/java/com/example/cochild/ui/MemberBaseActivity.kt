package com.example.cochild.ui

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.cochild.AdminTeamBoardActivity
import com.example.cochild.MemberTeamCalendarActivity
import com.example.cochild.AdminTeamNoticeActivity
import com.example.cochild.MemberTeamBoardActivity
import com.example.cochild.AdminTeamListActivity
import com.example.cochild.MemberTeamHomeActivity
import com.example.cochild.MemberTeamListActivity
import com.example.cochild.MemberTeamNoticeActivity
import com.example.cochild.MyPageActivity
import com.example.cochild.R
import com.example.cochild.utils.TeamIdHelper
import com.example.cochild.utils.TeamUtils
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth

open class MemberBaseActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    protected fun setupBottomNavigationView(selectedMenuId: Int, teamId: String) {
        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNavigationView.selectedItemId = selectedMenuId

        bottomNavigationView.setOnItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_home -> {
                    if (selectedMenuId != R.id.nav_home) {
                        TeamIdHelper.startActivityWithTeamId(this, MemberTeamHomeActivity::class.java, teamId)
                    }
                    true
                }
                R.id.nav_board -> {
                    if (selectedMenuId != R.id.nav_board) {
                        TeamIdHelper.startActivityWithTeamId(this, MemberTeamBoardActivity::class.java, teamId)
                    }
                    true
                }
                R.id.nav_calendar -> {
                    if (selectedMenuId != R.id.nav_calendar) {
                        TeamIdHelper.startActivityWithTeamId(this, MemberTeamCalendarActivity::class.java, teamId)
                    }
                    true
                }
                R.id.nav_notice -> {
                    if(selectedMenuId != R.id.nav_notice){
                        TeamIdHelper.startActivityWithTeamId(this, MemberTeamNoticeActivity::class.java, teamId)
                    }
                    true
                }
                else -> false
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_member, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_mypage -> {
                startActivity(Intent(this, MyPageActivity::class.java))
                true
            }
            R.id.action_memberlist -> {
                val teamId = TeamIdHelper.getTeamId(this)
                if (teamId != null) {
                    // TEAM_ID가 있을 경우 AdminTeamList로 이동
                    TeamIdHelper.startActivityWithTeamId(this, MemberTeamListActivity::class.java, teamId)
                } else {
                    // TEAM_ID가 없을 경우 메시지 출력
                    Toast.makeText(this, "팀 ID를 찾을 수 없습니다.", Toast.LENGTH_SHORT).show()
                }
                true
            }
            R.id.action_exit_team -> {
                showExitTeamDialog()
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

    protected fun setupToolbar() {
        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.top_bar)
        setSupportActionBar(toolbar)
    }

    private fun showExitTeamDialog() {
        AlertDialog.Builder(this)
            .setTitle("팀 탈퇴")
            .setMessage("정말로 팀을 탈퇴하시겠습니까?")
            .setPositiveButton("탈퇴") { _, _ -> exitTeam() }
            .setNegativeButton("취소", null)
            .show()
    }

    private fun exitTeam() {
        val teamId = TeamIdHelper.getTeamId(this)
        val currentUser = FirebaseAuth.getInstance().currentUser

        if (teamId.isNullOrEmpty() || currentUser == null) {
            Toast.makeText(this, "팀 정보를 불러오지 못했습니다.", Toast.LENGTH_SHORT).show()
            return
        }

        val userId = currentUser.uid

        TeamUtils.removeTeamMember(
            context = this,
            teamId = teamId,
            memberId = userId,
            onSuccess = {
//                Toast.makeText(this, "팀을 성공적으로 탈퇴했습니다.", Toast.LENGTH_SHORT).show()
                finish() // 탈퇴 후 액티비티 종료
            },
            onFailure = { exception ->
                Toast.makeText(this, "팀 탈퇴 실패: ${exception.message}", Toast.LENGTH_SHORT).show()
                Log.e("MemberBaseActivity", "팀 탈퇴 실패: ${exception.message}")
            }
        )
    }

}

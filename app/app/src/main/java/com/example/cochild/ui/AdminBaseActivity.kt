package com.example.cochild

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.cochild.utils.TeamUtils
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.example.cochild.utils.TeamIdHelper
import com.google.firebase.firestore.FirebaseFirestore

open class AdminBaseActivity : AppCompatActivity() {

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

                        TeamIdHelper.startActivityWithTeamId(this, AdminTeamHomeActivity::class.java, teamId)
                    }
                    true
                }
                R.id.nav_board -> {
                    if (selectedMenuId != R.id.nav_board) {

                        TeamIdHelper.startActivityWithTeamId(this, AdminTeamBoardActivity::class.java, teamId)
                    }
                    true
                }
                R.id.nav_calendar -> {
                    if (selectedMenuId != R.id.nav_calendar) {

                        TeamIdHelper.startActivityWithTeamId(this, AdminTeamCalendarActivity::class.java, teamId)

                    }
                    true
                }
                R.id.nav_notice -> {
                    if(selectedMenuId != R.id.nav_notice){

                        TeamIdHelper.startActivityWithTeamId(this, AdminTeamNoticeActivity::class.java, teamId)

                    }
                    true
                }
                else -> false
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_admin, menu)
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
                    TeamIdHelper.startActivityWithTeamId(this, AdminTeamListActivity::class.java, teamId)
                } else {
                    // TEAM_ID가 없을 경우 메시지 출력
                    Toast.makeText(this, "팀 ID를 찾을 수 없습니다.", Toast.LENGTH_SHORT).show()
                }
                true
            }
            R.id.action_remove_team -> {
                val teamId = TeamIdHelper.getTeamId(this) // 현재 팀 ID 가져오기
                if (teamId != null) {
                    // Firestore에서 팀 문서를 불러와 삭제 작업 수행
                    val firestore = FirebaseFirestore.getInstance()
                    firestore.collection("teams").document(teamId).get()
                        .addOnSuccessListener { document ->
                            if (document != null && document.exists()) {
                                val profileImageUrl = document.getString("profileImage") // 프로필 이미지 URL 가져오기

                                // 삭제 확인 다이얼로그
                                androidx.appcompat.app.AlertDialog.Builder(this)
                                    .setTitle("팀 삭제")
                                    .setMessage("정말 팀을 삭제하시겠습니까? 이 작업은 되돌릴 수 없습니다.")
                                    .setPositiveButton("삭제") { _, _ ->
                                        // TeamDeleteUtils를 사용하여 팀 삭제
                                        TeamUtils.deleteTeam(
                                            context = this,
                                            teamId = teamId,
                                            profileImageUrl = profileImageUrl,
                                            onSuccess = {
                                                Toast.makeText(this, "팀이 성공적으로 삭제되었습니다.", Toast.LENGTH_SHORT).show()
                                                // 팀 삭제 후 홈 화면으로 이동
                                                startActivity(Intent(this, MainHomeActivity::class.java))
                                                finish()
                                            },
                                            onFailure = { errorMessage ->
                                                Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show()
                                            }
                                        )
                                    }
                                    .setNegativeButton("취소", null)
                                    .show()
                            } else {
                                Toast.makeText(this, "팀 정보를 찾을 수 없습니다.", Toast.LENGTH_SHORT).show()
                            }
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(this, "팀 정보를 불러오는 중 오류 발생: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                } else {
                    Toast.makeText(this, "팀 ID를 찾을 수 없습니다.", Toast.LENGTH_SHORT).show()
                }
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    protected fun setupToolbar() {
        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.top_bar)
        setSupportActionBar(toolbar)
    }

}

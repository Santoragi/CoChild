package com.example.cochild.alarm

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.widget.Button
import androidx.appcompat.app.AlertDialog
import com.example.cochild.R

class AdminTeamWithdrawAlarm(private val context: Context) {

    fun show() {
        // dialog_admin_team_withdraw.xml 레이아웃을 인플레이트하여 다이얼로그 뷰로 설정
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_admin_team_withdraw, null)

        // AlertDialog 빌더 설정
        val dialog = AlertDialog.Builder(context)
            .setView(dialogView)
            .create()

        // YES 버튼과 NO 버튼 찾기
        val yesButton = dialogView.findViewById<Button>(R.id.btn_yes)
        val noButton = dialogView.findViewById<Button>(R.id.btn_no)

        // YES 버튼 클릭 리스너 설정
        yesButton.setOnClickListener {
            Log.d("Yes", "팀 탈퇴를 진행합니다.")
            // 팀 탈퇴 진행 작업을 여기에 추가
            dialog.dismiss()  // 다이얼로그 닫기
        }

        // NO 버튼 클릭 리스너 설정
        noButton.setOnClickListener {
            Log.d("No", "팀 탈퇴를 취소합니다.")
            dialog.dismiss()  // 다이얼로그 닫기
        }

        // 다이얼로그 표시
        //dialog.show()
    }
}

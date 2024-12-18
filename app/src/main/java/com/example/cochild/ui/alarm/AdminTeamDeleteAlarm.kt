package com.example.cochild.alarm

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.widget.Button
import androidx.appcompat.app.AlertDialog
import com.example.cochild.R

class AdminTeamDeleteAlarm(private val context: Context) {

    fun show() {

        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_admin_team_delete, null)

        // AlertDialog 생성
        val dialog = AlertDialog.Builder(context)
            .setView(dialogView)
            .create()

        // YES 버튼 클릭 시 처리
        val yesButton = dialogView.findViewById<Button>(R.id.btn_yes)
        val noButton = dialogView.findViewById<Button>(R.id.btn_no)

        yesButton.setOnClickListener {
            // 팀 삭제 로직 추가
            // 예를 들어 팀을 삭제하는 API 호출 등을 넣을 수 있습니다.
            Log.d("Yes", "네 버튼을 클릭하셨습니다.")
            dialog.dismiss()  // 다이얼로그 닫기
        }

        // NO 버튼 클릭 시 처리
        noButton.setOnClickListener {
            // 다이얼로그 닫기
            Log.d("No", "아니요 버튼을 클릭하셨습니다.")
            dialog.dismiss()
        }

        // 다이얼로그 보여주기
       // dialog.show()
    }
}

package com.example.cochild.alarm

import android.content.Context
import android.util.Log
import androidx.appcompat.app.AlertDialog
import android.view.LayoutInflater
import android.widget.Button
import android.widget.TextView
import com.example.cochild.R

class AdminMemberDeleteAlarm(
    private val context: Context,
    private val name: String,
    private val onDeleteConfirmed: () -> Unit
) {

    fun show() {
        // dialog_admin_mem_delete.xml 레이아웃을 인플레이트
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_admin_mem_delete, null)

        // 다이얼로그 객체 생성
        val dialog = AlertDialog.Builder(context)
            .setView(dialogView)
            .create()

        val memberDeleteMessage = dialogView.findViewById<TextView>(R.id.mem_delete_message)
        memberDeleteMessage.text = "  정말 ${name}님을 추방하시겠습니까?"

        // 버튼 참조
        val yesButton = dialogView.findViewById<Button>(R.id.btn_yes)
        val noButton = dialogView.findViewById<Button>(R.id.btn_no)

        // YES 버튼 클릭 리스너
        yesButton.setOnClickListener {
            Log.d("AdminMemberDeleteAlarm", "네 버튼을 클릭하셨습니다.")
            onDeleteConfirmed() // 콜백 호출
            dialog.dismiss()  // 다이얼로그 닫기
        }

        // NO 버튼 클릭 리스너
        noButton.setOnClickListener {
            Log.d("No", "아니요 버튼을 클릭하셨습니다.")
            // 다이얼로그 닫기
            dialog.dismiss()
        }

        dialog.show()
    }
}

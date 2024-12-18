package com.example.cochild.utils

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast

object TeamIdHelper {

    private const val TEAM_ID_KEY = "TEAM_ID" // TEAM_ID 키 값 상수로 정의

    /**
     * A: TEAM_ID를 Intent에 추가하여 B로 이동하는 함수
     *
     * @param context 현재 Context
     * @param targetActivityClass 이동할 액티비티 클래스
     * @param teamId 전달할 TEAM_ID 값
     */
    fun startActivityWithTeamId(
        context: Context,
        targetActivityClass: Class<*>,
        teamId: String
    ) {
        val intent = Intent(context, targetActivityClass)
        intent.putExtra(TEAM_ID_KEY, teamId) // TEAM_ID 추가
        context.startActivity(intent)
        Log.d("TeamIdHelper", "Starting activity ${targetActivityClass.simpleName} with TEAM_ID: $teamId")
    }

    /**
     * B: Intent에서 TEAM_ID를 꺼내오는 함수
     *
     * @param activity 현재 액티비티
     * @return TEAM_ID 문자열 (없으면 null)
     */
    fun getTeamId(activity: Activity): String? {
        val teamId = activity.intent.getStringExtra(TEAM_ID_KEY)
        if (teamId.isNullOrEmpty()) {
            Toast.makeText(activity, "TEAM_ID를 찾을 수 없습니다.", Toast.LENGTH_SHORT).show()
            Log.e("TeamIdHelper", "TEAM_ID가 null 또는 비어 있습니다.")
            activity.finish() // TEAM_ID가 없을 경우 액티비티 종료
        } else {
            Log.d("TeamIdHelper", "Retrieved TEAM_ID: $teamId")
        }
        return teamId
    }
}

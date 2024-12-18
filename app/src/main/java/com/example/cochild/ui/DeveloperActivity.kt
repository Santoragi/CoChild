package com.example.cochild

import android.os.Bundle
import android.widget.ImageView
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.cochild.R
import com.example.cochild.BaseActivity

class DeveloperActivity : BaseActivity(){
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.developer)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.developer)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets

        }
        //상단바
        setupToolbar()

        // 뒤로가기 아이콘 클릭 시 MainHome 액티비티로 돌아가기
        val backIcon: ImageView = findViewById(R.id.backIcon)
        backIcon.setOnClickListener {
            // 뒤로 가기
            onBackPressed()
        }

    }
}
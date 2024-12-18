package com.example.cochild

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import com.example.cochild.DeveloperActivity
import com.example.cochild.MyPageActivity
import com.example.cochild.R

open class BaseActivity : AppCompatActivity() {

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_mypage -> {
                startActivity(Intent(this, MyPageActivity::class.java))
                true
            }
            R.id.action_developer -> {
                startActivity(Intent(this, DeveloperActivity::class.java))
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
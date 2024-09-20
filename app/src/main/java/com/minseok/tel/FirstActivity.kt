package com.minseok.tel

import android.content.Intent
import android.os.Bundle
import android.text.format.DateFormat
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class FirstActivity : AppCompatActivity() {
    private lateinit var tvTime: TextView
    private lateinit var timeFormat: SimpleDateFormat

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_first)  // 'activity_first.xml' 레이아웃 파일을 사용

        tvTime = findViewById(R.id.tvTime)

        val pattern = if (DateFormat.is24HourFormat(this)) "HH:mm" else "h:mm a"
        timeFormat = SimpleDateFormat(pattern, Locale.getDefault())

        updateTime()  // 초기 시간 설정
        startSplashScreen()
    }

    private fun updateTime() {
        val currentTime = timeFormat.format(Date())
        tvTime.text = currentTime
    }

    private fun startSplashScreen() {
        lifecycleScope.launch {
            delay(2000)  // 2초 대기
            navigateToMainActivity()
        }
    }

    private fun navigateToMainActivity() {
        val intent = Intent(this@FirstActivity, MainActivity::class.java)
        startActivity(intent)
        finish()  // FirstActivity 종료

        // 화면 전환 애니메이션 설정 (선택사항)
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }
}
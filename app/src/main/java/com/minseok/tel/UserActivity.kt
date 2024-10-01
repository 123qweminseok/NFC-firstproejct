package com.minseok.tel

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.google.android.material.navigation.NavigationView
import com.google.firebase.database.FirebaseDatabase
import com.minseok.tel.databinding.ActivityUserBinding

// UserActivity 클래스: 사용자 메인 화면을 관리하는 액티비티
class UserActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    // 뷰 바인딩 객체
    private lateinit var binding: ActivityUserBinding
    // DrawerLayout 객체
    private lateinit var drawerLayout: DrawerLayout
    // 사용자 이름
    private lateinit var name: String
    // 사용자 전화번호
    private lateinit var phoneNumber: String
    // 메시지 버튼
    private lateinit var nextmessage: ImageButton
    // 출석 체크 버튼
    private lateinit var attendcheck: ImageButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 뷰 바인딩 초기화
        binding = ActivityUserBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // DrawerLayout 초기화
        drawerLayout = binding.drawerLayout
        val navigationView: NavigationView = binding.navView
        val toolbar = binding.toolbar

        // 툴바를 액션바로 설정
        setSupportActionBar(toolbar)
        supportActionBar?.title = "" // 기본 타이틀 제거

        // 네비게이션 뷰의 아이템 선택 리스너 설정
        navigationView.setNavigationItemSelectedListener(this)

//        toolbar.setTitleTextColor(resources.getColor(android.R.color.black)) // 타이틀 색상을 검정으로 변경
//        toolbar.setSubtitleTextColor(resources.getColor(android.R.color.black)) // 서브타이틀 색상을 검정으로 변경 ->코드로 색상변경하는것.


        // ActionBarDrawerToggle 설정 (햄버거 아이콘 표시 및 동작 처리)
        val toggle = ActionBarDrawerToggle(
            this, drawerLayout, toolbar,
            R.string.navigation_drawer_open, R.string.navigation_drawer_close
        )
        // 햄버거 메뉴 아이콘의 색상을 검정색으로 설정

        toggle.drawerArrowDrawable.color = resources.getColor(android.R.color.black)

        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        // 버튼 초기화
        nextmessage = findViewById(R.id.message)
        attendcheck = findViewById(R.id.check)

        // Intent에서 사용자 정보 가져오기
        val getIntent = intent
        phoneNumber = getIntent.getStringExtra("PHONE_NUMBER") ?: ""
        name = getIntent.getStringExtra("Name") ?: ""


        // 클릭 리스너 설정
        setupClickListeners()

        updateNavigationHeader()

    }

    private fun updateNavigationHeader() {
        val navigationView = findViewById<NavigationView>(R.id.nav_view)
        val headerView = navigationView.getHeaderView(0)
        val navHeaderImage = headerView.findViewById<ImageView>(R.id.nav_header_image)
        val navHeaderName = headerView.findViewById<TextView>(R.id.nav_header_name)

        Log.d("UserActivity", "Updating navigation header")

        // Firebase 실시간 데이터베이스에서 사용자 정보 가져오기
        val database = FirebaseDatabase.getInstance()
        val userRef = database.getReference("users").child(phoneNumber)

        userRef.get().addOnSuccessListener { dataSnapshot ->
            if (dataSnapshot.exists()) {
                val userData = dataSnapshot.value as? Map<String, Any>
                val userName = userData?.get("name") as? String ?: "Unknown User"
                val imageUrl = userData?.get("imageUrl") as? String

                // 사용자 이름 설정
                navHeaderName.text = userName

                // 이미지 로드
                if (!imageUrl.isNullOrEmpty()) {
                    Glide.with(this@UserActivity)
                        .load(imageUrl)
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .placeholder(R.drawable.default_profile)
                        .error(R.drawable.default_profile)
                        .circleCrop()
                        .into(navHeaderImage)
                } else {
                    navHeaderImage.setImageResource(R.drawable.default_profile)
                }
            } else {
                Log.d("UserActivity", "User data does not exist")
                navHeaderName.text = "Unknown User"
                navHeaderImage.setImageResource(R.drawable.default_profile)
            }
        }.addOnFailureListener { e ->
            Log.e("UserActivity", "Error loading user data", e)
            navHeaderName.text = "Error loading user data"
            navHeaderImage.setImageResource(R.drawable.default_profile)
        }
    }


    // 버튼 클릭 리스너 설정 메서드
    private fun setupClickListeners() {
        // 출석 체크 버튼 클릭 시
        attendcheck.setOnClickListener {
            startAttendanceCheckActivity()
        }

        // 메시지 버튼 클릭 시
        nextmessage.setOnClickListener {
            openMessageApp()
        }

        // 모바일 신분증 버튼 클릭 시
        binding.id.setOnClickListener {
            startMobileSinActivity()
        }

        // 출석 버튼 클릭 시
        binding.attendance.setOnClickListener {
            startAttendanceActivity()
        }
    }

    // 네비게이션 드로어 메뉴 아이템 선택 처리
        override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.nav_check -> startAttendanceCheckActivity()
            R.id.nav_attendance -> startAttendanceActivity()
            R.id.nav_message -> openMessageApp()
            R.id.nav_id -> startMobileSinActivity()
        }
        // 메뉴 선택 후 드로어 닫기
        drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }

    // 출석 체크 액티비티 시작 메서드
    private fun startAttendanceCheckActivity() {
        val intent = Intent(this, AttendanceCheck::class.java).apply {
            putExtra("PHONE_NUMBER", phoneNumber)
            putExtra("Name", name)
        }
        startActivity(intent)
    }

    // 출석 액티비티 시작 메서드
    private fun startAttendanceActivity() {
        val intent = Intent(this, AttendanceActivity::class.java).apply {
            putExtra("PHONE_NUMBER", phoneNumber)
            putExtra("Name", name)
        }
        startActivity(intent)
    }

    // 메시지 앱 열기 메서드
    private fun openMessageApp() {
        val messageFragment = MessageFragment.newInstance("그냥넣음", "사실필요없긴")//MessageFragment 내부에서 사용될수 있는데 데이터 ㅇㅇ
        supportFragmentManager.beginTransaction()
            .setCustomAnimations(
                R.anim.slide_in_right,
                R.anim.slide_out_left,
                R.anim.slide_in_left,
                R.anim.slide_out_right
            )
            .add(R.id.fragment_container, messageFragment)
            .addToBackStack(null)
            .commit()
    }

    // 모바일 신분증 액티비티 시작 메서드
    private fun startMobileSinActivity() {
        val intent = Intent(this, MoblieSin::class.java)
        startActivity(intent)
    }

    // 뒤로 가기 버튼 처리
    override fun onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            // 드로어가 열려있으면 닫기
            drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            // 그렇지 않으면 기본 뒤로 가기 동작 수행
            super.onBackPressed()
        }
    }
}
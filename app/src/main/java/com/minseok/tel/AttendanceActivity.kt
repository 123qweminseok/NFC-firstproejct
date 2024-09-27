package com.minseok.tel

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec
import android.util.Log
import javax.crypto.BadPaddingException

class AttendanceActivity : AppCompatActivity() {

    private lateinit var phoneNumber: String
    private lateinit var database: DatabaseReference
    private lateinit var recyclerView: RecyclerView
    private lateinit var attendanceAdapter: AttendanceAdapter
    private val attendanceList = mutableListOf<AttendanceItem>()
    private lateinit var secretKey: SecretKey

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_attendance)

        phoneNumber = intent.getStringExtra("PHONE_NUMBER") ?: ""
        //val firebaseUrl = "https://haha-f3b7a-default-rtdb.firebaseio.com/" //김민석
        val firebaseUrl = "https://nfckt-b7c41-default-rtdb.firebaseio.com/" //이희우
        database = FirebaseDatabase.getInstance(firebaseUrl).reference
        secretKey = loadKey()

        recyclerView = findViewById(R.id.recyclerViewAttendance)
        attendanceAdapter = AttendanceAdapter(attendanceList)
        recyclerView.adapter = attendanceAdapter
        recyclerView.layoutManager = LinearLayoutManager(this)

        loadAttendanceData()
    }

    private fun loadAttendanceData() {
        val encryptedPhoneNumber = EncryptionUtil.encrypt(phoneNumber, secretKey)
        database.child("attendance").orderByChild("phoneNumber").equalTo(encryptedPhoneNumber)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    for (data in snapshot.children) {
                        try {
                            // 복호화된 데이터를 화면에 추가
                            val encryptedName = data.child("name").getValue(String::class.java) ?: ""
                            val encryptedPhoneNumber = data.child("phoneNumber").getValue(String::class.java) ?: ""
                            val encryptedTimestamp = data.child("timestamp").getValue(String::class.java) ?: ""

                            val name = EncryptionUtil.decrypt(encryptedName, secretKey)
                            val phoneNumber = EncryptionUtil.decrypt(encryptedPhoneNumber, secretKey)
                            val timestamp = EncryptionUtil.decrypt(encryptedTimestamp, secretKey)

                            attendanceList.add(AttendanceItem(name, phoneNumber, timestamp))
                        } catch (e: BadPaddingException) {
                            Log.e("EncryptionError", "Error decrypting data", e)
                        }
                    }
                    attendanceAdapter.notifyDataSetChanged()
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@AttendanceActivity, "Failed to load attendance: ${error.message}", Toast.LENGTH_SHORT).show()
                    Log.e("FirebaseError", "Error fetching attendance data", error.toException())
                }
            })
    }

    private fun loadKey(): SecretKey {
        val fixedKeyString = "12345678901234567890123456789012" // 고정된 키
        val fixedKey = fixedKeyString.toByteArray(Charsets.UTF_8)
        return SecretKeySpec(fixedKey, "AES")
    }
}

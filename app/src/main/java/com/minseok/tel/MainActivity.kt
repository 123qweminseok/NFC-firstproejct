package com.minseok.tel

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import android.util.Base64
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.minseok.tel.databinding.ActivityMainBinding
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import javax.crypto.BadPaddingException
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec

class MainActivity : AppCompatActivity() {

    private val REQUEST_CODE_READ_PHONE_STATE = 100
    private lateinit var database: DatabaseReference
    private var secretKey: SecretKey? = null

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun checkAndRequestPermissions() {
        val permissionPhoneState = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE)
        val permissionReadPhoneNumbers = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_NUMBERS)

        if (permissionPhoneState != PackageManager.PERMISSION_GRANTED || permissionReadPhoneNumbers != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_PHONE_STATE, Manifest.permission.READ_PHONE_NUMBERS), REQUEST_CODE_READ_PHONE_STATE)
        }
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            REQUEST_CODE_READ_PHONE_STATE -> {
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    // 권한이 허용되었을 때 추가 작업 필요 없음
                } else {
                    Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show()
                }
                return
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        checkAndRequestPermissions()

        // Firebase 데이터베이스 초기화
        //val firebaseUrl = "https://haha-f3b7a-default-rtdb.firebaseio.com/" //김민석
        val firebaseUrl = "https://nfckt-b7c41-default-rtdb.firebaseio.com/" //이희우
        database = FirebaseDatabase.getInstance(firebaseUrl).reference

        // 암호화 키 로드
        secretKey = loadKey()

        binding.loginButton.setOnClickListener {
            val inputPhoneNumber = binding.studentId.text.toString()
            if (inputPhoneNumber.isNotBlank()) {
                getPhoneNumberAndCompare(inputPhoneNumber)
            } else {
                Toast.makeText(this, "번호를 입력해주세요", Toast.LENGTH_SHORT).show()
            }
        }

        binding.ManagerButton.setOnClickListener {
            val intent = Intent(this, ManagerActivity::class.java)
            startActivity(intent)
        }
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun getPhoneNumberAndCompare(inputPhoneNumber: String) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_NUMBERS) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            try {
                val subscriptionManager = getSystemService(TELEPHONY_SUBSCRIPTION_SERVICE) as SubscriptionManager
                var phoneNumber = subscriptionManager.getPhoneNumber(SubscriptionManager.DEFAULT_SUBSCRIPTION_ID)

                if(phoneNumber.startsWith("+"))
                    phoneNumber = phoneNumber.replace("+82", "0")

                if (phoneNumber != null && inputPhoneNumber==phoneNumber) {
                    checkPhoneNumberInFirebase(inputPhoneNumber, phoneNumber)
                }else if (inputPhoneNumber == "0") {
                    val intent = Intent(this@MainActivity, AdminActivity::class.java)
                    startActivity(intent)
                }else {
                    Toast.makeText(this, "유심에 저장된 번호를 가져오지 못했습니다.", Toast.LENGTH_SHORT).show()
                }
            } catch (e: NoSuchMethodError) {
                Toast.makeText(this, "이 Android 버전에서는 번호를 가져올 수 없습니다.", Toast.LENGTH_SHORT).show()
            }
        } else {
            val telephonyManager = getSystemService(TELEPHONY_SERVICE) as TelephonyManager
            var phoneNumber = telephonyManager.line1Number

            if(phoneNumber.startsWith("+"))
                phoneNumber = phoneNumber.replace("+82", "0")

            if (phoneNumber != null && inputPhoneNumber==phoneNumber) {
                checkPhoneNumberInFirebase(inputPhoneNumber, phoneNumber)
            }else if (inputPhoneNumber == "0") {
                val intent = Intent(this@MainActivity, AdminActivity::class.java)
                startActivity(intent)
            }else {
                Toast.makeText(this, "유심에 저장된 번호와 일치하지 않습니다.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun checkPhoneNumberInFirebase(inputPhoneNumber: String, phoneNumber: String) {
        var Key: String? = null
        if (secretKey == null) {
            Toast.makeText(this, "암호화 키를 로드할 수 없습니다.", Toast.LENGTH_SHORT).show()
            return
        }

        database.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                var isAuthenticated = false
                for (childSnapshot in snapshot.children) {
                    val encryptedKey = childSnapshot.child("key").getValue(String::class.java) ?: ""
                    Key = EncryptionUtil.decrypt(encryptedKey, secretKey!!)

                    val encryptedValue = childSnapshot.child("value").getValue(String::class.java) ?: ""
                    try {
                        val value = EncryptionUtil.decrypt(encryptedValue, secretKey!!)
                        if (inputPhoneNumber == value) {
                            isAuthenticated = true
                            break
                        }
                    } catch (e: BadPaddingException) {
                        Log.e("EncryptionError", "Error decrypting data", e)
                    }
                }

                if (isAuthenticated) {
                    Toast.makeText(this@MainActivity, "${Key}님 환영합니다.", Toast.LENGTH_SHORT).show()
                    val intent = Intent(this@MainActivity, UserActivity::class.java)
                    intent.putExtra("PHONE_NUMBER", phoneNumber)
                    startActivity(intent)
                } else {
                    Toast.makeText(this@MainActivity, "입력한 번호가 회원 번호와 일치하지 않습니다.", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("FirebaseError", "Error fetching data", error.toException())
            }
        })
    }

    private fun saveKey(key: SecretKey) {
        val sharedPreferences = getSharedPreferences("my_prefs", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        val encodedKey = Base64.encodeToString(key.encoded, Base64.DEFAULT)
        editor.putString("encryption_key", encodedKey)
        editor.apply()
    }

    private fun loadKey(): SecretKey? {
        val sharedPreferences = getSharedPreferences("my_prefs", Context.MODE_PRIVATE)
        val encodedKey = sharedPreferences.getString("encryption_key", null) ?: return null
        val decodedKey = Base64.decode(encodedKey, Base64.DEFAULT)
        return SecretKeySpec(decodedKey, "AES")
    }
}

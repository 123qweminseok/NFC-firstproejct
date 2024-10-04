package com.minseok.tel
import androidx.appcompat.app.AlertDialog
import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import android.util.Base64
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.TextView
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
    private lateinit var binding: ActivityMainBinding
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var save_credentials: CheckBox

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
                    //ㅇㅇ 비어둠
                }
                return
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // SharedPreferences 초기화
        sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)

        // 체크박스 연결
        save_credentials = binding.saveCredentials

        // 저장된 전화번호 불러오기
        val savedPhoneNumber = sharedPreferences.getString("SAVED_PHONE_NUMBER", "")
        binding.studentId.setText(savedPhoneNumber)

        // 체크박스 상태 설정
        val isChecked = sharedPreferences.getBoolean("CHECKBOX_STATE", false)
        save_credentials.isChecked = isChecked

        // 체크박스 클릭 리스너
        save_credentials.setOnCheckedChangeListener { _, isChecked ->
            val editor = sharedPreferences.edit()
            editor.putBoolean("CHECKBOX_STATE", isChecked)

            // 체크가 되어 있을 경우에만 전화번호를 저장하고, 체크가 해제될 경우 전화번호를 초기화
            if (isChecked) {
                editor.putString("SAVED_PHONE_NUMBER", binding.studentId.text.toString())
            } else {
                editor.remove("SAVED_PHONE_NUMBER") // 전화번호 초기화
            }

            editor.apply()
        }

        checkAndRequestPermissions()

        // Firebase 데이터베이스 초기화
        //val firebaseUrl = "https://haha-f3b7a-default-rtdb.firebaseio.com/" //김민석
        val firebaseUrl = "https://nfckt-b7c41-default-rtdb.firebaseio.com/" //이희우
        database = FirebaseDatabase.getInstance(firebaseUrl).reference

        // 암호화 키 로드
        secretKey = loadKey()

        // 유심 번호를 가져와서 Firebase에서 permission 확인
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                try {
                    val subscriptionManager = getSystemService(TELEPHONY_SUBSCRIPTION_SERVICE) as SubscriptionManager
                    var phoneNumber = subscriptionManager.getPhoneNumber(SubscriptionManager.DEFAULT_SUBSCRIPTION_ID)

                    if (phoneNumber != null && phoneNumber.startsWith("+")) {
                        phoneNumber = phoneNumber.replace("+82", "0")
                    }

                    if (phoneNumber != null) {
                        checkPermissionInFirebase(phoneNumber) // 유심 번호로 Firebase permission 확인
                    } else {
                        Toast.makeText(this, "유심 번호를 가져올 수 없습니다.", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: NoSuchMethodError) {
                    Toast.makeText(this, "이 Android 버전에서는 번호를 가져올 수 없습니다.", Toast.LENGTH_SHORT).show()
                }
            } else {
                val telephonyManager = getSystemService(TELEPHONY_SERVICE) as TelephonyManager
                var phoneNumber = telephonyManager.line1Number

                if (phoneNumber != null && phoneNumber.startsWith("+")) {
                    phoneNumber = phoneNumber.replace("+82", "0")
                }

                if (phoneNumber != null) {
                    checkPermissionInFirebase(phoneNumber)
                } else {
                    Toast.makeText(this, "유심 번호를 가져올 수 없습니다.", Toast.LENGTH_SHORT).show()
                }
            }
        }

        binding.loginButton.setOnClickListener {
            val inputPhoneNumber = binding.studentId.text.toString()
            if (inputPhoneNumber.isNotBlank()) {
                // 전화번호 저장
                if (save_credentials.isChecked) {
                    val editor = sharedPreferences.edit()
                    editor.putString("SAVED_PHONE_NUMBER", inputPhoneNumber)
                    editor.apply()
                }
                getPhoneNumberAndCompare(inputPhoneNumber)
            } else {
                showAlertDialog("번호를 입력해주세요")
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
                    showAlertDialog("유심에 저장된 번호와 일치하지 않습니다.")
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
                showAlertDialog("유심에 저장된 번호와 일치하지 않습니다.")
            }
        }
    }
    private fun showAlertDialog(message: String) {
        val builder = AlertDialog.Builder(this)
        val inflater = layoutInflater
        val dialogLayout = inflater.inflate(R.layout.custom_dialog, null)

        val titleText = dialogLayout.findViewById<TextView>(R.id.dialogTitle)
        val messageText = dialogLayout.findViewById<TextView>(R.id.dialogMessage)
        val okButton = dialogLayout.findViewById<Button>(R.id.dialogButton)

        titleText.text = "알림"
        messageText.text = message

        builder.setView(dialogLayout)
        val dialog = builder.create()

        okButton.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()

        // 다이얼로그 크기 조정
        val displayMetrics = resources.displayMetrics
        val width = (displayMetrics.widthPixels * 0.9).toInt()
        dialog.window?.setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT)
    }//로그인 실패 !! 다이얼로그


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
                    intent.putExtra("Name",Key)
                    startActivity(intent)
                } else {
                    showAlertDialog("입력한 번호가 회원 번호와 일치하지 않습니다.")
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("FirebaseError", "Error fetching data", error.toException())
            }
        })
    }

    private fun loadKey(): SecretKey {
        val fixedKeyString = "12345678901234567890123456789012" // 고정된 키
        val fixedKey = fixedKeyString.toByteArray(Charsets.UTF_8)
        return SecretKeySpec(fixedKey, "AES")
    }

    // Firebase에서 유심 번호와 일치하는 permission 확인
    private fun checkPermissionInFirebase(phoneNumber: String) {
        database.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                var isAdmin = false
                for (childSnapshot in snapshot.children) {
                    val encryptedValue = childSnapshot.child("value").getValue(String::class.java) ?: ""
                    val decryptedValue = EncryptionUtil.decrypt(encryptedValue, secretKey!!) // 전화번호 복호화

                    if (phoneNumber == decryptedValue) {
                        val encryptedPermission = childSnapshot.child("permission").getValue(String::class.java) ?: ""
                        val decryptedPermission = EncryptionUtil.decrypt(encryptedPermission, secretKey!!) // permission 복호화

                        if (decryptedPermission == "admin") {
                            isAdmin = true
                            break
                        }
                    }
                }

                // 매니저 버튼의 가시성 설정
                    binding.ManagerButton.visibility = if (true) View.VISIBLE else View.GONE
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("FirebaseError", "Error fetching data", error.toException())
            }
        })
    }

}

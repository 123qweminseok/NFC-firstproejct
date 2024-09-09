package com.minseok.tel

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import android.view.View
import android.view.View.OnClickListener
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.minseok.tel.databinding.ActivityMainBinding


class MainActivity : AppCompatActivity() {

    lateinit  var ManagerButton:Button //관리자 버튼

    private val REQUEST_CODE_READ_PHONE_STATE = 100

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

        binding.button.setOnClickListener {
            val inputPhoneNumber = binding.editTextPhone.text.toString()
            if (inputPhoneNumber.isNotBlank()) {
                getPhoneNumberAndCompare(inputPhoneNumber)
            } else {
                Toast.makeText(this, "번호를 입력해주세요", Toast.LENGTH_SHORT).show()
            }
        }


        binding.ManagerButton.setOnClickListener{

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
                    phoneNumber=phoneNumber.replace("+82","0")

                if (phoneNumber != null) {
                    if (inputPhoneNumber == phoneNumber) {
                        Toast.makeText(this, "입력한 번호가 유심에 저장된 번호와 일치합니다.", Toast.LENGTH_SHORT).show()
                        val intent = Intent(this, UserActivity::class.java)
                        intent.putExtra("PHONE_NUMBER", phoneNumber)
                        startActivity(intent)
                    }else if(inputPhoneNumber.equals("0")){
                        val intent = Intent(this, AdminActivity::class.java)
                        startActivity(intent)
                    }else {
                        Toast.makeText(this, "입력한 번호가 유심에 저장된 번호와 일치하지 않습니다.", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this, "유심에 저장된 번호를 가져오지 못했습니다.", Toast.LENGTH_SHORT).show()
                }
            } catch (e: NoSuchMethodError) {
                Toast.makeText(this, "이 Android 버전에서는 번호를 가져올 수 없습니다.", Toast.LENGTH_SHORT).show()
            }
        } else {
            val telephonyManager = getSystemService(TELEPHONY_SERVICE) as TelephonyManager
            var phoneNumber = telephonyManager.line1Number

            if(phoneNumber.startsWith("+"))
                phoneNumber=phoneNumber.replace("+82","0")

            if (phoneNumber != null) {
                if (inputPhoneNumber == phoneNumber) {
                    Toast.makeText(this, "입력한 번호가 유심에 저장된 번호와 일치합니다.", Toast.LENGTH_SHORT).show()
                    val intent = Intent(this, UserActivity::class.java)
                    intent.putExtra("PHONE_NUMBER", phoneNumber)
                    startActivity(intent)
                }else if(inputPhoneNumber.equals("0")){
                    val intent = Intent(this, AdminActivity::class.java)
                    startActivity(intent)
                }else {
                    Toast.makeText(this, "입력한 번호가 유심에 저장된 번호와 일치하지 않습니다.", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "유심에 저장된 번호를 가져오지 못했습니다.", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
package com.minseok.tel

import android.app.PendingIntent
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Shader
import android.net.Uri
import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.Ndef
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.minseok.tel.databinding.ActivityAttendanceCheckBinding
import com.minseok.tel.databinding.ActivityUserBinding
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec


class AttendanceCheck : AppCompatActivity() {
    private lateinit var nfcAdapter: NfcAdapter
    private lateinit var pendingIntent: PendingIntent
    private lateinit var intentFilters: Array<IntentFilter>
    private lateinit var phoneNumber: String
    private lateinit var database: DatabaseReference
    private lateinit var secretKey: SecretKey
    private lateinit var gifImage1: ImageView



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        var binding= ActivityAttendanceCheckBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //val firebaseUrl = "https://haha-f3b7a-default-rtdb.firebaseio.com/" //김민석
        val firebaseUrl = "https://nfckt-b7c41-default-rtdb.firebaseio.com/" //이희우
        database = FirebaseDatabase.getInstance(firebaseUrl).reference

        // 암호화 키 로드
        secretKey = loadKey()

       gifImage1=findViewById<ImageView>(R.id.imageView4)
        Glide.with(this).asGif().load(R.drawable.redermove).into(gifImage1)
//이 코드를 추가함으로 gif이미지가 이제 움직일 수 있게 됨


        nfcAdapter = NfcAdapter.getDefaultAdapter(this)
        if (nfcAdapter == null) {
            Toast.makeText(this, "NFC is not available on this device.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        val getIntent = getIntent()
        phoneNumber = getIntent.getStringExtra("PHONE_NUMBER") ?: ""

        // Create an Intent to handle the NFC data
        val intent = Intent(this, javaClass)
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_MUTABLE)

        // Create an IntentFilter for NFC tag discovery
        val tagDetected = IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED)
        intentFilters = arrayOf(tagDetected)



        val textView = findViewById<TextView>(R.id.textView4)

        // 그라데이션 효과 적용
        val paint = textView.paint
        val width = paint.measureText(textView.text.toString())
        val textShader = LinearGradient(0f, 0f, width, textView.textSize, intArrayOf(
            Color.parseColor("#FFD700"),  // 금색
            Color.parseColor("#FFA500"),  // 주황색
            Color.parseColor("#FFD700")   // 다시 금색
        ), null, Shader.TileMode.CLAMP)
        textView.paint.shader = textShader

        // 애니메이션 적용
        val animation = AnimationUtils.loadAnimation(this, R.anim.text_animation)
        textView.startAnimation(animation)
    }

    override fun onResume() {
        super.onResume()
        nfcAdapter.enableForegroundDispatch(this, pendingIntent, intentFilters, null)
    }

    override fun onPause() {
        super.onPause()
        nfcAdapter.disableForegroundDispatch(this)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleNfcIntent(intent)
    }

    private fun handleNfcIntent(intent: Intent) {
        val action = intent.action
        if (NfcAdapter.ACTION_TAG_DISCOVERED == action) {
            val tag = intent.getParcelableExtra<Tag>(NfcAdapter.EXTRA_TAG)
            tag?.let {
                writePhoneNumberToTag(it)
            }
        }
    }

    private fun writePhoneNumberToTag(tag: Tag) {
        val ndef = Ndef.get(tag) ?: run {
            Toast.makeText(this, "NDEF is not supported on this tag.", Toast.LENGTH_SHORT).show()
            saveAttendance(phoneNumber) // 전화번호 저장
            return
        }
        val message = NdefMessage(
            arrayOf(
                NdefRecord.createTextRecord("en", phoneNumber)  // Creating a text record with the phone number
            )
        )
        try {
            ndef.connect()
            if (!ndef.isWritable) {
                Toast.makeText(this, "Tag is not writable.", Toast.LENGTH_SHORT).show()
                return
            }
            if (ndef.maxSize < message.toByteArray().size) {
                Toast.makeText(this, "Message is too large for the tag.", Toast.LENGTH_SHORT).show()
                return
            }
            ndef.writeNdefMessage(message)
            Toast.makeText(this, "Phone number written to tag.", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Failed to write to tag.", Toast.LENGTH_SHORT).show()
        } finally {
            ndef.close()
        }
    }

    private fun saveAttendance(phoneNumber: String) {
        val encryptedPhoneNumber = EncryptionUtil.encrypt(phoneNumber, secretKey)

        val currentTime = System.currentTimeMillis()
        val timestamp = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date(currentTime))

        val encryptedTimestmp = EncryptionUtil.encrypt(timestamp, secretKey)

        val attendanceData = mapOf(
            "phoneNumber" to encryptedPhoneNumber,
            "timestamp" to encryptedTimestmp
        )

        database.child("attendance").push().setValue(attendanceData)
            .addOnSuccessListener {
                Toast.makeText(this, "Attendance saved successfully.", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "Failed to save attendance: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun loadKey(): SecretKey {
        val fixedKeyString = "12345678901234567890123456789012" // 고정된 키
        val fixedKey = fixedKeyString.toByteArray(Charsets.UTF_8)
        return SecretKeySpec(fixedKey, "AES")
    }
}
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
    import android.widget.TextView
    import android.widget.Toast
    import androidx.appcompat.app.AppCompatActivity
    import com.google.firebase.database.DatabaseReference
    import com.google.firebase.database.FirebaseDatabase
    import com.minseok.tel.databinding.ActivityUserBinding
    import javax.crypto.SecretKey
    import javax.crypto.spec.SecretKeySpec

    class UserActivity : AppCompatActivity() {

        private lateinit var nfcAdapter: NfcAdapter
        private lateinit var pendingIntent: PendingIntent
        private lateinit var intentFilters: Array<IntentFilter>
        private lateinit var phoneNumber: String
        private lateinit var nextmessage: ImageButton
        private lateinit var mobileImage: ImageButton
        private lateinit var database: DatabaseReference
        private lateinit var secretKey: SecretKey
        private lateinit var attendcheck: ImageButton

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            var binding = ActivityUserBinding.inflate(layoutInflater)
            setContentView(binding.root)
            nextmessage = findViewById(R.id.message)
            attendcheck = findViewById(R.id.check)

            val getIntent = getIntent()
            phoneNumber = getIntent.getStringExtra("PHONE_NUMBER") ?: ""


            attendcheck.setOnClickListener {
                val intent = Intent(this, AttendanceCheck::class.java)
                intent.putExtra("PHONE_NUMBER", phoneNumber)
                startActivity(intent)
            }

            nextmessage.setOnClickListener(View.OnClickListener {
                val intent = Intent(Intent.ACTION_SENDTO).apply {
                    data = Uri.parse("smsto:")  // This ensures only SMS apps respond
                }
                try {
                    startActivity(intent)
                } catch (e: ActivityNotFoundException) {
                    Toast.makeText(this, "SMS 앱을 찾을 수 없습니다.", Toast.LENGTH_SHORT).show()
                }

            })

            binding.id.setOnClickListener {
                val intent = Intent(this, MoblieSin::class.java)
                startActivity(intent)
            }

            binding.attendance.setOnClickListener {
                val intent = Intent(this, AttendanceActivity::class.java)
                intent.putExtra("PHONE_NUMBER", phoneNumber)
                startActivity(intent)
            }

        }
    }
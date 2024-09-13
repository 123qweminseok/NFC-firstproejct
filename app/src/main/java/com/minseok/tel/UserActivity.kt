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

    class UserActivity : AppCompatActivity() {

        private lateinit var nfcAdapter: NfcAdapter
        private lateinit var pendingIntent: PendingIntent
        private lateinit var intentFilters: Array<IntentFilter>
        private lateinit var phoneNumber: String
        private lateinit var firebaseDatabase: FirebaseDatabase
        private lateinit var databaseReference: DatabaseReference
        private lateinit var nextmessage: ImageButton
        private lateinit var mobileImage: ImageButton

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            var binding= ActivityUserBinding.inflate(layoutInflater)
            setContentView(binding.root)
            nextmessage=findViewById(R.id.message)
            firebaseDatabase = FirebaseDatabase.getInstance();
            databaseReference = firebaseDatabase.getReference();

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

            binding.id.setOnClickListener{

                val intent = Intent(this, MoblieSin::class.java)
                startActivity(intent)
            }



            val textView = findViewById<TextView>(R.id.textView3)

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
                databaseReference.child("phoneNumber").push().setValue(phoneNumber)
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
    }
package com.minseok.tel

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.telephony.TelephonyManager
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.database.*
import com.minseok.tel.databinding.FragmentMessageperceptionBinding
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec

//사용자 인증 과정
//메시지 암호화/복호화 과정
//Firebase 데이터베이스 상호작용
//권한 요청 및 처리
//UI 업데이트 로직
//authenticateUser 에서 파이어베이스 내 이름 가져옴. 내 번호 암호화해서 파베 db랑 비교해서 같으면 가져옴.
class Messageperception : Fragment() {
    // 뷰 바인딩을 위한 변수
    private var _binding: FragmentMessageperceptionBinding? = null
    private val binding get() = _binding!!

    // Firebase 데이터베이스 참조
    private lateinit var database: DatabaseReference
    // 암호화/복호화에 사용할 비밀 키
    private lateinit var secretKey: SecretKey
    // 메시지 목록을 표시할 어댑터
    private lateinit var messageAdapter: MessageAdapter
    // 현재 사용자가 관리자인지 여부
    private var isAdmin = false
    // 현재 사용자의 전화번호와 이름
    private var currentUserPhone: String? = null
    private var currentUserName: String? = null

    // 프래그먼트의 뷰를 생성하고 반환
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMessageperceptionBinding.inflate(inflater, container, false)
        return binding.root
    }

    // 뷰가 생성된 후 호출되는 메서드
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Firebase 초기화
        val firebaseUrl = "https://haha-f3b7a-default-rtdb.firebaseio.com/"
        database = FirebaseDatabase.getInstance(firebaseUrl).reference

        // 암호화 키 로드
        secretKey = loadKey()

        // RecyclerView 초기화
        messageAdapter = MessageAdapter { message -> deleteMessage(message) }
        binding.messageRecyclerView.layoutManager = LinearLayoutManager(context)
        binding.messageRecyclerView.adapter = messageAdapter

        // 현재 사용자의 전화번호 가져오기
        currentUserPhone = getCurrentPhoneNumber()
        if (currentUserPhone != null) {
            // 전화번호로 사용자 인증
            authenticateUser(currentUserPhone!!)
        } else {
            Toast.makeText(context, "전화번호를 가져올 수 없습니다.", Toast.LENGTH_SHORT).show()
        }

        // 메시지 전송 버튼 리스너 설정
        binding.sendButton.setOnClickListener {
            val message = binding.messageEditText.text.toString()
            if (message.isNotBlank() && currentUserName != null && currentUserPhone != null) {
                sendMessage(message)
                binding.messageEditText.text.clear()
            } else {
                Toast.makeText(context, "메시지를 입력하거나 사용자 인증을 기다려주세요.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // 현재 기기의 전화번호를 가져오는 함수
    private fun getCurrentPhoneNumber(): String? {
        // 전화번호 읽기 권한 확인
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            // 권한이 없으면 요청
            ActivityCompat.requestPermissions(requireActivity(), arrayOf(Manifest.permission.READ_PHONE_STATE), PERMISSION_REQUEST_READ_PHONE_STATE)
            return null
        }

        return try {
            val telephonyManager = requireContext().getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
            var phoneNumber = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) telephonyManager.line1Number else {
                @Suppress("DEPRECATION")
                telephonyManager.line1Number
            }

            // 국제 번호 형식(+82)을 국내 번호 형식(0)으로 변환
            if (phoneNumber?.startsWith("+82") == true) {
                phoneNumber = "0" + phoneNumber.substring(3)
            }
            phoneNumber
        } catch (e: Exception) {
            Log.e("Messageperception", "Error getting phone number", e)
            null
        }
    }

    // 사용자 인증 함수
    private fun authenticateUser(phoneNumber: String) {
        // 전화번호 암호화
        val encryptedPhoneNumber = EncryptionUtil.encrypt(phoneNumber, secretKey)
        // Firebase에서 암호화된 전화번호로 사용자를 찾고, 권한을 확인합니다.
        database.orderByChild("value").equalTo(encryptedPhoneNumber)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        for (childSnapshot in snapshot.children) {
                            // 사용자 정보 복호화
                            val encryptedName = childSnapshot.child("key").getValue(String::class.java) ?: ""
                            val encryptedPermission = childSnapshot.child("permission").getValue(String::class.java) ?: ""

                            currentUserName = EncryptionUtil.decrypt(encryptedName, secretKey)
                            val permission = EncryptionUtil.decrypt(encryptedPermission, secretKey)

                            // 관리자 여부 설정
                            isAdmin = permission == "admin"
                            updateUI()
                            if (isAdmin) {
                                fetchMessages()
                            }
                            Log.d("Messageperception", "User authenticated: name=$currentUserName, phone=$currentUserPhone, isAdmin=$isAdmin")
                            break
                        }
                    } else {
                        Toast.makeText(context, "인증된 사용자가 아닙니다.", Toast.LENGTH_SHORT).show()
                        Log.e("Messageperception", "User not found: $phoneNumber")
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(context, "사용자 인증 중 오류가 발생했습니다.", Toast.LENGTH_SHORT).show()
                    Log.e("Messageperception", "Authentication error", error.toException())
                }
            })
    }

    // UI 업데이트 함수
    private fun updateUI() {
        if (isAdmin) {
            // 관리자 UI: 메시지 목록 표시
            binding.userLayout.visibility = View.GONE
            binding.messageRecyclerView.visibility = View.VISIBLE
        } else {
            // 일반 사용자 UI: 메시지 입력 폼 표시
            binding.userLayout.visibility = View.VISIBLE
            binding.messageRecyclerView.visibility = View.GONE
        }
    }

    // 메시지 전송 함수
    private fun sendMessage(message: String) {
        // 메시지 내용과 사용자 정보 암호화
        val encryptedMessage = EncryptionUtil.encrypt(message, secretKey)
        val encryptedSender = EncryptionUtil.encrypt(currentUserName ?: "", secretKey)
        val encryptedPhone = EncryptionUtil.encrypt(currentUserPhone ?: "", secretKey)

        val messageData = hashMapOf(
            "sender" to encryptedSender,
            "phone" to encryptedPhone,
            "message" to encryptedMessage,
            "timestamp" to ServerValue.TIMESTAMP
        )

        // Firebase에 메시지 저장
        database.child("messages").push().setValue(messageData)
            .addOnSuccessListener {
                Toast.makeText(context, "메시지가 전송되었습니다.", Toast.LENGTH_SHORT).show()
                Log.d("Messageperception", "Message sent successfully")
            }
            .addOnFailureListener {
                Toast.makeText(context, "메시지 전송에 실패했습니다.", Toast.LENGTH_SHORT).show()
                Log.e("Messageperception", "Failed to send message", it)
            }
    }

    // 메시지 목록 가져오기 함수 (관리자용)
    private fun fetchMessages() {
        database.child("messages").addChildEventListener(object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                try {
                    // 메시지 데이터 복호화
                    val message = Message()
                    message.id = snapshot.key
                    message.sender = EncryptionUtil.decrypt(snapshot.child("sender").getValue(String::class.java) ?: "", secretKey)
                    message.phone = EncryptionUtil.decrypt(snapshot.child("phone").getValue(String::class.java) ?: "", secretKey)
                    message.message = EncryptionUtil.decrypt(snapshot.child("message").getValue(String::class.java) ?: "", secretKey)
                    message.timestamp = snapshot.child("timestamp").getValue(Long::class.java) ?: 0

                    messageAdapter.addMessage(message)
                    Log.d("Messageperception", "Fetched message: sender=${message.sender}, phone=${message.phone}, message=${message.message}")
                } catch (e: Exception) {
                    Log.e("Messageperception", "Error decrypting message", e)
                }
            }

            // 다른 이벤트 핸들러 메서드 (변경 없음)
            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onChildRemoved(snapshot: DataSnapshot) {
                val messageId = snapshot.key
                messageId?.let { messageAdapter.removeMessage(it) }
            }
            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    // 메시지 삭제 함수
    private fun deleteMessage(message: Message) {
        message.id?.let { id ->
            database.child("messages").child(id).removeValue()
                .addOnSuccessListener {
                    Toast.makeText(context, "메시지가 삭제되었습니다.", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener {
                    Toast.makeText(context, "메시지 삭제에 실패했습니다.", Toast.LENGTH_SHORT).show()
                }
        }
    }

    // 암호화 키 로드 함수
    private fun loadKey(): SecretKey {
        val fixedKeyString = "12345678901234567890123456789012" // 고정된 키
        val fixedKey = fixedKeyString.toByteArray(Charsets.UTF_8)
        return SecretKeySpec(fixedKey, "AES")
    }

    // 권한 요청 결과 처리
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        when (requestCode) {
            PERMISSION_REQUEST_READ_PHONE_STATE -> {
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    // 권한이 부여되면 전화번호를 가져오고 사용자 인증 진행
                    currentUserPhone = getCurrentPhoneNumber()
                    currentUserPhone?.let { authenticateUser(it) }
                } else {
                    Toast.makeText(context, "전화번호를 가져올 수 있는 권한이 없습니다.", Toast.LENGTH_LONG).show()
                }
                return
            }
        }
    }

    // 프래그먼트 뷰가 파괴될 때 호출
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null // 메모리 누수 방지
    }

    companion object {
        private const val PERMISSION_REQUEST_READ_PHONE_STATE = 1
    }
}
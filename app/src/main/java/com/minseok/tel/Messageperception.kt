package com.minseok.tel

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.database.*
import com.minseok.tel.databinding.FragmentMessageperceptionBinding
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec

class Messageperception : Fragment() {
    private var _binding: FragmentMessageperceptionBinding? = null
    private val binding get() = _binding!!

    private lateinit var database: DatabaseReference
    private lateinit var secretKey: SecretKey
    private lateinit var messageAdapter: MessageAdapter
    private var isAdmin = false
    private var currentUserPhone: String? = null
    private var currentUserName: String? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMessageperceptionBinding.inflate(inflater, container, false)
        return binding.root
    }

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

        // 현재 사용자 정보 가져오기
        currentUserPhone = arguments?.getString("PHONE_NUMBER")
        currentUserName = arguments?.getString("NAME")

        // 사용자 권한 확인
        checkUserPermission()

        // 메시지 전송 버튼 리스너
        binding.sendButton.setOnClickListener {
            val message = binding.messageEditText.text.toString()
            if (message.isNotBlank()) {
                sendMessage(message)
                binding.messageEditText.text.clear()
            }


            navigateToUserActivity()


        }
    }
    private fun navigateToUserActivity() {
        // UserActivity로 이동하는 Intent 생성
        val intent = Intent(activity, UserActivity::class.java)

        // 필요한 경우, 현재 사용자 정보를 Intent에 추가
        intent.putExtra("PHONE_NUMBER", currentUserPhone)
        intent.putExtra("NAME", currentUserName)

        // 플래그 추가: 이전 액티비티들을 모두 종료하고 새로운 태스크 시작
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK

        startActivity(intent)

        // 선택적: 현재 액티비티 종료 (프래그먼트를 포함하고 있는 액티비티)
        activity?.finish()
    }

    private fun checkUserPermission() {
        database.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                var foundUser = false
                for (childSnapshot in snapshot.children) {
                    val encryptedPermission = childSnapshot.child("permission").getValue(String::class.java) ?: ""
                    val permission = EncryptionUtil.decrypt(encryptedPermission, secretKey)
                    when (permission) {
                        "admin" -> {
                            isAdmin = true
                            updateUI()
                            fetchMessages()
                            return
                        }
                        "user" -> {
                            foundUser = true
                        }
                    }
                }
                if (foundUser) {
                    isAdmin = false
                    updateUI()
                } else {
                    Toast.makeText(context, "유효한 사용자를 찾을 수 없습니다.", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(context, "권한 확인 중 오류가 발생했습니다.", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun updateUI() {
        if (isAdmin) {
            binding.userLayout.visibility = View.GONE
            binding.messageRecyclerView.visibility = View.VISIBLE
        } else {
            binding.userLayout.visibility = View.VISIBLE
            binding.messageRecyclerView.visibility = View.GONE
        }
    }
    private fun sendMessage(message: String) {
        val encryptedMessage = EncryptionUtil.encrypt(message, secretKey)
        val encryptedSender = EncryptionUtil.encrypt(currentUserName ?: "", secretKey)
        val encryptedPhone = EncryptionUtil.encrypt(currentUserPhone ?: "", secretKey)

        val messageData = hashMapOf(
            "sender" to encryptedSender,
            "phone" to encryptedPhone,
            "message" to encryptedMessage,
            "timestamp" to ServerValue.TIMESTAMP
        )

        database.child("messages").push().setValue(messageData)
            .addOnSuccessListener {
                Toast.makeText(context, "메시지가 전송되었습니다.", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(context, "메시지 전송에 실패했습니다.", Toast.LENGTH_SHORT).show()
            }
    }


    //메시지 가져옴. db에있는 메시지.
    private fun fetchMessages() {
        database.child("messages").addChildEventListener(object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                try {
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

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onChildRemoved(snapshot: DataSnapshot) {
                val messageId = snapshot.key
                messageId?.let { messageAdapter.removeMessage(it) }
            }
            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onCancelled(error: DatabaseError) {}
        })
    }

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

    private fun loadKey(): SecretKey {
        val fixedKeyString = "12345678901234567890123456789012" // 고정된 키
        val fixedKey = fixedKeyString.toByteArray(Charsets.UTF_8)
        return SecretKeySpec(fixedKey, "AES")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
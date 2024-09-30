package com.minseok.tel

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import com.minseok.tel.databinding.ActivityMobliesinBinding
import java.util.UUID

class MoblieSin : AppCompatActivity() {

    private lateinit var binding: ActivityMobliesinBinding
    private lateinit var phoneNumber: String
    private var selectedImageUri: Uri? = null
    private val PICK_IMAGE_REQUEST = 1

    private val storage = FirebaseStorage.getInstance()
    private val storageRef = storage.reference
    private val database = FirebaseDatabase.getInstance()
    private val databaseRef = database.reference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMobliesinBinding.inflate(layoutInflater)
        setContentView(binding.root)

        phoneNumber = intent.getStringExtra("PHONE_NUMBER") ?: ""

        binding.selectImageButton.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            startActivityForResult(intent, PICK_IMAGE_REQUEST)
        }

        binding.saveButton.setOnClickListener {
            val name = binding.nameEditText.text.toString()
            val residentNumber = binding.residentNumberEditText.text.toString()
            val address = binding.addressEditText.text.toString()

            if (name.isNotEmpty() && residentNumber.isNotEmpty() && address.isNotEmpty()) {
                saveUserData(name, residentNumber, address)
            } else {
                Toast.makeText(this, "모든 필드를 입력해주세요.", Toast.LENGTH_SHORT).show()
            }
        }

        loadUserData()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null && data.data != null) {
            selectedImageUri = data.data
            binding.selectImageButton.text = "이미지 선택됨"
            loadImageWithGlide(selectedImageUri)
        }
    }
    private fun loadImageWithGlide(imageUri: Uri?) {
        Glide.with(this)
            .load(imageUri)
            .apply(RequestOptions().circleCrop())
            .into(binding.userImageView)
    }

    private fun saveUserData(name: String, residentNumber: String, address: String) {
        val imageRef = storageRef.child("images/$phoneNumber")
        val uploadTask = selectedImageUri?.let { imageRef.putFile(it) }

        uploadTask?.continueWithTask { task ->
            if (!task.isSuccessful) {
                task.exception?.let { throw it }
            }
            imageRef.downloadUrl
        }?.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val imageUrl = task.result.toString()
                val userData = hashMapOf(
                    "name" to name,
                    "phoneNumber" to phoneNumber,
                    "residentNumber" to residentNumber,
                    "address" to address,
                    "imageUrl" to imageUrl
                )
                databaseRef.child("users").child(phoneNumber).setValue(userData)
                    .addOnSuccessListener {
                        Toast.makeText(this, "데이터가 저장되었습니다.", Toast.LENGTH_SHORT).show()

                        // UserActivity로 돌아가기
                        val intent = Intent(this, UserActivity::class.java)
                        intent.putExtra("PHONE_NUMBER", phoneNumber)
                        intent.putExtra("Name", name)
                        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                        startActivity(intent)
                        finish()
                    }.addOnFailureListener {
                        Toast.makeText(this, "데이터 저장에 실패했습니다.", Toast.LENGTH_SHORT).show()
                    }

            } else {
                Toast.makeText(this, "이미지 업로드에 실패했습니다.", Toast.LENGTH_SHORT).show()
            }
        } ?: run {
            // 이미지가 선택되지 않았을 경우
            val userData = hashMapOf(
                "name" to name,
                "phoneNumber" to phoneNumber,
                "residentNumber" to residentNumber,
                "address" to address
            )

            databaseRef.child("users").child(phoneNumber).setValue(userData)
                .addOnSuccessListener {
                    Toast.makeText(this, "데이터가 저장되었습니다.", Toast.LENGTH_SHORT).show()
                }.addOnFailureListener {
                    Toast.makeText(this, "데이터 저장에 실패했습니다.", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun loadUserData() {
        databaseRef.child("users").child(phoneNumber).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val userData = snapshot.value as? Map<String, Any>
                    userData?.let {
                        binding.nameEditText.setText(it["name"] as? String ?: "")
                        binding.residentNumberEditText.setText(it["residentNumber"] as? String ?: "")
                        binding.addressEditText.setText(it["address"] as? String ?: "")
                        val imageUrl = it["imageUrl"] as? String
                        imageUrl?.let { url ->
                            loadImageWithGlide(Uri.parse(url))
                        }
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@MoblieSin, "데이터 로드에 실패했습니다.", Toast.LENGTH_SHORT).show()
            }
        })
    }
}
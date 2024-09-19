package com.minseok.tel

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.*
import javax.crypto.BadPaddingException
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec

class ManagerActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var dataAdapter: DataAdapter
    private lateinit var database: DatabaseReference

    private lateinit var editTextKey: EditText
    private lateinit var editTextValue: EditText
    private lateinit var buttonConfirm: Button

    private lateinit var secretKey: SecretKey

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_manager)

        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        dataAdapter = DataAdapter(emptyList())
        recyclerView.adapter = dataAdapter

        editTextKey = findViewById(R.id.editTextKey)
        editTextValue = findViewById(R.id.editTextValue)
        buttonConfirm = findViewById(R.id.buttonConfirm)

        //val firebaseUrl = "https://haha-f3b7a-default-rtdb.firebaseio.com/" //김민석
        val firebaseUrl = "https://nfckt-b7c41-default-rtdb.firebaseio.com/" //이희우
        database = FirebaseDatabase.getInstance(firebaseUrl).reference

        secretKey = loadKey() ?: run {
            val newKey = EncryptionUtil.generateKey()
            saveKey(newKey)
            newKey
        }

        buttonConfirm.setOnClickListener {
            val key = editTextKey.text.toString()
            val value = editTextValue.text.toString()
            saveDataToFirebase(key, value)
            editTextKey.text.clear()
            editTextValue.text.clear()
        }

        fetchData()

    }

    private fun saveDataToFirebase(key: String, value: String) {
        // 먼저 데이터베이스에서 중복된 값이 있는지 확인
        database.orderByChild("value").equalTo(EncryptionUtil.encrypt(value, secretKey))
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        // 데이터가 이미 존재하면 저장하지 않음
                        Toast.makeText(this@ManagerActivity, "이미 존재하는 번호입니다.", Toast.LENGTH_SHORT).show()
                        Log.d("FirebaseData", "Value already exists in the database")
                    } else {
                        // 중복되지 않으면 저장
                        try {
                            val encryptedKey = EncryptionUtil.encrypt(key, secretKey)
                            val encryptedValue = EncryptionUtil.encrypt(value, secretKey)

                            val data = mapOf("key" to encryptedKey, "value" to encryptedValue)
                            database.push().setValue(data)
                                .addOnSuccessListener {
                                    Log.d("FirebaseData", "Data saved successfully")
                                }
                                .addOnFailureListener { exception ->
                                    Log.e("FirebaseError", "Error saving data", exception)
                                }
                        } catch (e: Exception) {
                            Log.e("EncryptionError", "Error encrypting data", e)
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("FirebaseError", "Error checking for existing data", error.toException())
                }
            })
    }


    private fun fetchData() {
        database.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val items = mutableListOf<DataItem>()
                for (childSnapshot in snapshot.children) {
                    val encryptedKey = childSnapshot.child("key").getValue(String::class.java) ?: ""
                    val encryptedValue = childSnapshot.child("value").getValue(String::class.java) ?: ""

                    try {
                        val key = EncryptionUtil.decrypt(encryptedKey, secretKey)
                        val value = EncryptionUtil.decrypt(encryptedValue, secretKey)
                        items.add(DataItem(key, value))
                        Log.d("FirebaseData", "Key: $key, Value: $value")
                    } catch (e: BadPaddingException) {
                        Log.e("EncryptionError", "Error decrypting data", e)
                    }
                }
                dataAdapter.updateItems(items)
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
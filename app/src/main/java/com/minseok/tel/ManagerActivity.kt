package com.minseok.tel

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.widget.Button
import android.widget.EditText
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

        //val firebaseUrl = "https://haha-f3b7a-default-rtdb.firebaseio.com/"
        val firebaseUrl = "https://haha-f3b7a-default-rtdb.firebaseio.com/" //이희우
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
package com.minseok.tel

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.*

class ManagerActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var dataAdapter: DataAdapter
    private lateinit var database: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_manager)

        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        dataAdapter = DataAdapter(emptyList())
        recyclerView.adapter = dataAdapter

        val firebaseUrl = "https://haha-f3b7a-default-rtdb.firebaseio.com/"
        database = FirebaseDatabase.getInstance(firebaseUrl).reference

        fetchData()

            //ㅎㅇㅎㅇ
    }

    private fun fetchData() {
        database.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val items = mutableListOf<DataItem>()
                for (childSnapshot in snapshot.children) {
                    val key = childSnapshot.key ?: ""
                    val value = childSnapshot.value.toString()
                    items.add(DataItem(key, value))
                    Log.d("FirebaseData", "Key: $key, Value: $value")
                }
                dataAdapter.updateItems(items)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("FirebaseError", "Error fetching data", error.toException())
            }
        })
    }
}
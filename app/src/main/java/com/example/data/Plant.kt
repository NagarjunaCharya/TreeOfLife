package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "plants")
data class Plant(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val imageUrl: String, // URI of internal image
    val diagnosis: String,
    val recommendation: String,
    val timestamp: Long = System.currentTimeMillis()
)

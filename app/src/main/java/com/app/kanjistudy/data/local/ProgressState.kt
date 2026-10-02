package com.app.kanjistudy.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "progress_state")
data class ProgressState(@PrimaryKey val id: Int = 0)

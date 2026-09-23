package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.dao.LudoDao
import com.example.data.entity.GameAchievement
import com.example.data.entity.MatchHistory
import com.example.data.entity.PlayerProfile

@Database(
    entities = [PlayerProfile::class, MatchHistory::class, GameAchievement::class],
    version = 1,
    exportSchema = false
)
abstract class LudoDatabase : RoomDatabase() {
    abstract fun ludoDao(): LudoDao

    companion object {
        @Volatile
        private var INSTANCE: LudoDatabase? = null

        fun getDatabase(context: Context): LudoDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    LudoDatabase::class.java,
                    "ludo_royale_db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}

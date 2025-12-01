package com.example.myapplication.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.myapplication.data.dao.QRCodeDao
import com.example.myapplication.data.model.QRCodeEntity

@Database(
    entities = [QRCodeEntity::class],
    version = 2,
    exportSchema = false
)
abstract class QRCodeDatabase : RoomDatabase() {
    
    abstract fun qrCodeDao(): QRCodeDao
    
    companion object {
        @Volatile
        private var INSTANCE: QRCodeDatabase? = null
        
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Add isFavorite column
                database.execSQL("ALTER TABLE qr_codes ADD COLUMN isFavorite INTEGER NOT NULL DEFAULT 0")
                // Add tags column
                database.execSQL("ALTER TABLE qr_codes ADD COLUMN tags TEXT")
            }
        }
        
        fun getDatabase(context: Context): QRCodeDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    QRCodeDatabase::class.java,
                    "qr_code_database"
                )
                .addMigrations(MIGRATION_1_2)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}


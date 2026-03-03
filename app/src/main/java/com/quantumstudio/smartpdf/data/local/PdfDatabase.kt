package com.quantumstudio.smartpdf.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.quantumstudio.smartpdf.data.model.PdfFile

@Database(entities = [PdfFile::class], version = 1, exportSchema = false)
abstract class PdfDatabase : RoomDatabase() {

    abstract fun pdfFileDao(): PdfFileDao

    companion object {
        @Volatile
        private var INSTANCE: PdfDatabase? = null

        fun getDatabase(context: Context): PdfDatabase {
            // 如果 INSTANCE 不为空直接返回，否则同步创建
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    PdfDatabase::class.java,
                    "pdf_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
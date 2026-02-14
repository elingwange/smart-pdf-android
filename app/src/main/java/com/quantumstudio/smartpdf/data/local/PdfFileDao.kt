package com.quantumstudio.smartpdf.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.quantumstudio.smartpdf.data.model.PdfFile

@Dao
interface PdfFileDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(pdfs: List<PdfFile>)

    @Query("SELECT * FROM pdf_files ORDER BY lastModified DESC")
    suspend fun getAllPdfs(): List<PdfFile>

    // 可选：添加清理重复数据的测试方法
    @Query("DELETE FROM pdf_files")
    suspend fun deleteAll()
}
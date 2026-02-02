package com.quantumstudio.smartpdf.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.quantumstudio.smartpdf.data.model.PdfFile

@Dao
interface PdfFileDao {
    @Query("SELECT * FROM pdf_files ORDER BY lastModified DESC")
    suspend fun getAllPdfs(): List<PdfFile>

    // 使用 REPLACE 确保文件信息更新时不会因主键冲突崩溃
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(pdfs: List<PdfFile>)
}
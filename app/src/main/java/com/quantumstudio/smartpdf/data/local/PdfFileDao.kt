package com.quantumstudio.smartpdf.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.quantumstudio.smartpdf.data.model.PdfFile

@Dao
interface PdfFileDao {
    // 修改：由 REPLACE 改为 IGNORE
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(pdfs: List<PdfFile>)

    @Query("SELECT * FROM pdf_files ORDER BY lastModified DESC")
    suspend fun getAllPdfs(): List<PdfFile>

    @Query("UPDATE pdf_files SET isFavorite = :isFavorite WHERE path = :path")
    suspend fun updateFavorite(path: String, isFavorite: Boolean)

    @Query("DELETE FROM pdf_files")
    suspend fun deleteAll()

    // 更新最后阅读时间
    @Query("UPDATE pdf_files SET lastReadTime = :timestamp WHERE path = :path")
    suspend fun updateLastReadTime(path: String, timestamp: Long)

    // 获取最近阅读的文件（按时间倒序，取前 20 个）
    @Query("SELECT * FROM pdf_files WHERE lastReadTime > 0 ORDER BY lastReadTime DESC LIMIT 20")
    suspend fun getRecentPdfs(): List<PdfFile>
}
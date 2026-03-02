package com.quantumstudio.smartpdf.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.quantumstudio.smartpdf.data.model.PdfFile
import kotlinx.coroutines.flow.Flow

@Dao
interface PdfFileDao {
    // ✨ 必须添加：根据路径查询单个 PDF
    @Query("SELECT * FROM pdf_files WHERE path = :path LIMIT 1")
    suspend fun getPdfByPath(path: String): PdfFile?
    
    // 修改：由 REPLACE 改为 IGNORE
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(pdfs: List<PdfFile>)

    @Query("SELECT * FROM pdf_files ORDER BY lastModified DESC")
    suspend fun getAllPdfs(): List<PdfFile>

    @Query("UPDATE pdf_files SET isFavorite = :isFavorite WHERE path = :path")
    suspend fun updateFavorite(path: String, isFavorite: Boolean)

    @Query("DELETE FROM pdf_files")
    suspend fun deleteAll()

    @Query("UPDATE pdf_files SET lastReadTime = :timestamp WHERE path = :path")
    suspend fun updateLastReadTime(path: String, timestamp: Long)

    // 获取最近阅读的文件（按时间倒序，取前 20 个）
    @Query("SELECT * FROM pdf_files WHERE lastReadTime > 0 ORDER BY lastReadTime DESC LIMIT 20")
    suspend fun getRecentPdfs(): List<PdfFile>

    @Query("DELETE FROM pdf_files WHERE path = :path")
    suspend fun deleteByPath(path: String)

    @Query("SELECT * FROM pdf_files ORDER BY lastModified DESC")
    fun getAllPdfsFlow(): Flow<List<PdfFile>> // 移除 suspend，返回 Flow

    @Query("UPDATE pdf_files SET currentPage = :page, lastReadTime = :timestamp WHERE path = :path")
    suspend fun updatePageProgress(path: String, page: Int, timestamp: Long)


}
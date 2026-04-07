package com.quantumstudio.smartpdf.data.local

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RawQuery
import androidx.sqlite.db.SupportSQLiteQuery
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

    @Query("UPDATE pdf_files SET pages = :count WHERE path = :path AND pages = 0")
    suspend fun updatePageCount(path: String, count: Int)

    /**
     * 获取所有需要补全页数的文件
     * 限制返回数量（比如一次只取 100 个），防止内存溢出 (OOM)
     */
    @Query("SELECT * FROM pdf_files WHERE pages <= 0")
    suspend fun getFilesWithNoPages(): List<PdfFile>

    //---------------------- v0.7 --------------------------
    // 仅获取路径，极其节省内存，用于比对
    @Query("SELECT path FROM pdf_files")
    suspend fun getAllPaths(): List<String>

    // 批量删除，用于清理失效文件
    @Query("DELETE FROM pdf_files WHERE path IN (:paths)")
    suspend fun deleteByPaths(paths: List<String>)

    /**
     * 核心修改：使用 RawQuery 实现动态排序
     * observedEntities 必须指定，否则当外部扫描存入数据时，UI 不会自动刷新
     */
    @RawQuery(observedEntities = [PdfFile::class])
    fun getPdfsRaw(query: SupportSQLiteQuery): PagingSource<Int, PdfFile>
    //---------------------- v0.7 --------------------------
}
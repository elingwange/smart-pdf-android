package com.quantumstudio.smartpdf.data.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.sqlite.db.SimpleSQLiteQuery
import com.quantumstudio.smartpdf.data.local.PdfFileDao
import com.quantumstudio.smartpdf.data.model.PdfFile
import com.quantumstudio.smartpdf.data.scanner.PdfScanner2
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import javax.inject.Inject


class PdfRepository2 @Inject constructor(
    private val scanner: PdfScanner2,
    private val pdfDao: PdfFileDao
) {

    /**
     * 【核心读取流】UI 只通过此方法获取数据
     * 解决了低端机 4000+ 文件导致的内存溢出问题。
     */
    fun getPagingData(
        searchQuery: String,
        sortField: String,
        isAsc: Boolean
    ): Flow<PagingData<PdfFile>> {
        val order = if (isAsc) "ASC" else "DESC"

        // 1. 构建动态 SQL
        val sql = StringBuilder("SELECT * FROM pdf_files ")
        val args = mutableListOf<Any>()

        if (searchQuery.isNotBlank()) {
            sql.append("WHERE name LIKE ? ")
            args.add("%$searchQuery%")
        }

        sql.append("ORDER BY $sortField $order")
        val sqliteQuery = SimpleSQLiteQuery(sql.toString(), args.toTypedArray())

        // 2. 配置针对低端机的 PagingConfig
        return Pager(
            config = PagingConfig(
                pageSize = 15,           // 减小页大小，降低单次内存抖动
                prefetchDistance = 3,    // 缩短预取距离，节省低端机内存
                enablePlaceholders = false,
                initialLoadSize = 30     // 初始加载量也不宜过大
            ),
            pagingSourceFactory = { pdfDao.getPdfsRaw(sqliteQuery) }
        ).flow
    }

    /**
     * 【后台同步任务】负责将磁盘扫描结果同步到 DB
     * 此方法应在 ViewModel.init 或 WorkManager 中调用。
     */
    suspend fun syncDiskToDb() {
        withContext(Dispatchers.IO) {
            // 1. 快速扫描系统 MediaStore 索引
            val latestSystemPdfs = scanner.fetchMediaStoreIndex()

            // 2. 获取数据库中已有的路径集（用于差分对比）
            // 注意：低端机只查 path 字段，减少内存占用
            val cachedPaths = pdfDao.getAllPaths().toSet()

            // 3. 找出【新增】的文件并分批插入
            val newlyAdded = latestSystemPdfs.filter { it.path !in cachedPaths }
            if (newlyAdded.isNotEmpty()) {
                // 每 50 个一包，防止低端机数据库事务过大导致 UI 暂时卡死
                newlyAdded.chunked(50).forEach { chunk ->
                    pdfDao.insertAll(chunk) // 使用 OnConflictStrategy.IGNORE
                }
            }

            // 4. 找出【已删】的文件
            val latestPaths = latestSystemPdfs.map { it.path }.toSet()
            val removedPaths = cachedPaths.filter { it !in latestPaths }
            if (removedPaths.isNotEmpty()) {
                removedPaths.chunked(50).forEach { chunk ->
                    pdfDao.deleteByPaths(chunk)
                }
            }
        }
    }
}
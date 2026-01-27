package com.quantumstudio.smartpdf.pdf

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface PdfFileDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(pdf: PdfFile)

    @Update
    suspend fun update(pdf: PdfFile)

    @Delete
    suspend fun delete(pdf: PdfFile)

    @Query("SELECT * FROM pdf_files WHERE id = :id")
    suspend fun getById(id: String): PdfFile?

    @Query("SELECT * FROM pdf_files ORDER BY uploadTime DESC")
    suspend fun getAll(): List<PdfFile>

    @Query("SELECT * FROM pdf_files WHERE name LIKE '%' || :keyword || '%'")
    suspend fun searchByName(keyword: String): List<PdfFile>

    @Query("SELECT * FROM pdf_files WHERE category = :category")
    suspend fun searchByCategory(category: String): List<PdfFile>
}

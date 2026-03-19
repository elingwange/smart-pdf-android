package com.quantumstudio.smartpdf.di

import android.content.Context
import com.quantumstudio.smartpdf.data.local.PdfDatabase
import com.quantumstudio.smartpdf.data.local.PdfFileDao
import com.quantumstudio.smartpdf.data.repository.PdfRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton


@Module
@InstallIn(SingletonComponent::class) // 全局单例作用域
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): PdfDatabase {
        return PdfDatabase.getDatabase(context)
    }

    @Provides
    fun providePdfDao(database: PdfDatabase) = database.pdfFileDao()

    @Provides
    @Singleton
    fun provideRepository(dao: PdfFileDao): PdfRepository {
        return PdfRepository(dao)
    }

}
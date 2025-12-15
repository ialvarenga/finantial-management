package com.organizadorfinancas.di

import com.organizadorfinancas.data.parser.CsvStatementParser
import com.organizadorfinancas.data.parser.OfxStatementParser
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ParserModule {

    @Provides
    @Singleton
    fun provideCsvStatementParser(): CsvStatementParser {
        return CsvStatementParser()
    }

    @Provides
    @Singleton
    fun provideOfxStatementParser(): OfxStatementParser {
        return OfxStatementParser()
    }
}


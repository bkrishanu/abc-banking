package com.abcbank.mobile.feature.documents.di

import com.abcbank.mobile.feature.documents.data.AssetDocumentsSetup
import com.abcbank.mobile.feature.documents.data.FirestoreDocumentsRepository
import com.abcbank.mobile.feature.documents.data.PdfDocumentRenderer
import com.abcbank.mobile.feature.documents.domain.DocumentRenderer
import com.abcbank.mobile.feature.documents.domain.DocumentsRepository
import com.abcbank.mobile.feature.documents.domain.DocumentsSetupSource
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
interface DocumentsModule {
    @Binds
    fun bindDocumentsRepository(impl: FirestoreDocumentsRepository): DocumentsRepository

    @Binds
    fun bindDocumentsSetup(impl: AssetDocumentsSetup): DocumentsSetupSource

    @Binds
    fun bindDocumentRenderer(impl: PdfDocumentRenderer): DocumentRenderer
}

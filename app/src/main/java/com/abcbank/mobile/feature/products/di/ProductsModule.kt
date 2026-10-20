package com.abcbank.mobile.feature.products.di

import com.abcbank.mobile.feature.products.data.AssetProductCatalog
import com.abcbank.mobile.feature.products.data.FirestoreProductsRepository
import com.abcbank.mobile.feature.products.domain.ProductCatalog
import com.abcbank.mobile.feature.products.domain.ProductsRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
interface ProductsModule {
    @Binds
    fun bindProductsRepository(impl: FirestoreProductsRepository): ProductsRepository

    @Binds
    fun bindProductCatalog(impl: AssetProductCatalog): ProductCatalog
}

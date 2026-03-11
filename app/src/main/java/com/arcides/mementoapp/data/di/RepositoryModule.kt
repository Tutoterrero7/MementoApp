package com.arcides.mementoapp.data.di

import com.arcides.mementoapp.data.repositories.AuthRepositoryImpl
import com.arcides.mementoapp.data.repositories.CategoryRepositoryImpl
import com.arcides.mementoapp.data.repositories.TaskRepositoryImpl
import com.arcides.mementoapp.domain.repositories.AuthRepository
import com.arcides.mementoapp.domain.repositories.CategoryRepository
import com.arcides.mementoapp.domain.repositories.TaskRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindAuthRepository(
        impl: AuthRepositoryImpl): AuthRepository

    @Binds
    @Singleton
    abstract fun bindTaskRepository(impl: TaskRepositoryImpl): TaskRepository

    @Binds
    @Singleton
    abstract fun bindCategoryRepository(impl: CategoryRepositoryImpl): CategoryRepository

}

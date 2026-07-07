package com.example.smsforwarderpro.di

import com.example.smsforwarderpro.data.repository.MessageRepositoryImpl
import com.example.smsforwarderpro.data.repository.RuleRepositoryImpl
import com.example.smsforwarderpro.domain.repository.MessageRepository
import com.example.smsforwarderpro.domain.repository.RuleRepository
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
    abstract fun bindMessageRepository(
        impl: MessageRepositoryImpl
    ): MessageRepository

    @Binds
    @Singleton
    abstract fun bindRuleRepository(
        impl: RuleRepositoryImpl
    ): RuleRepository
}

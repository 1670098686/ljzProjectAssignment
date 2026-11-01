package com.campustrade.android.di

import android.content.Context
import com.campustrade.android.manager.PushNotificationManager
import com.campustrade.android.network.ApiService
import com.campustrade.android.network.BatchRequestManager
import com.campustrade.android.repository.UserRepository
import com.campustrade.android.repository.ProductRepository
import com.campustrade.android.service.BatchRequestService
import com.campustrade.android.util.SharedPreferencesHelper
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    
    @Singleton
    @Provides
    fun provideApiService(): ApiService {
        return ApiService.create()
    }
    
    @Singleton
    @Provides
    fun provideUserRepository(apiService: ApiService): UserRepository {
        return UserRepository(apiService)
    }
    
    @Singleton
    @Provides
    fun provideProductRepository(apiService: ApiService): ProductRepository {
        return ProductRepository(apiService)
    }
    
    @Singleton
    @Provides
    fun provideSharedPreferencesHelper(@ApplicationContext context: Context): SharedPreferencesHelper {
        return SharedPreferencesHelper(context)
    }
    
    @Singleton
    @Provides
    fun providePushNotificationManager(@ApplicationContext context: Context): PushNotificationManager {
        return PushNotificationManager(context)
    }
    
    @Singleton
    @Provides
    fun provideBatchRequestManager(): BatchRequestManager {
        return BatchRequestManager()
    }
    
    @Singleton
    @Provides
    fun provideBatchRequestService(
        apiService: ApiService,
        batchRequestManager: BatchRequestManager
    ): BatchRequestService {
        return BatchRequestService(apiService, batchRequestManager)
    }
}
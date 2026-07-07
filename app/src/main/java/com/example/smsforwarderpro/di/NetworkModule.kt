package com.example.smsforwarderpro.di

import android.content.Context
import android.content.pm.ApplicationInfo
import android.util.Log
import com.example.smsforwarderpro.data.network.GmailApi
import com.example.smsforwarderpro.data.network.TelegramApi
import com.example.smsforwarderpro.data.network.WhatsAppApi
import dagger.Module
import dagger.Provides
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideLoggingInterceptor(@ApplicationContext context: Context): HttpLoggingInterceptor {
        val isDebuggable = (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
        return HttpLoggingInterceptor { message ->
            Log.d("OkHttp", message.replace(Regex("bot\\d+:[^/\\s]+"), "bot<redacted>"))
        }.apply {
            level = if (isDebuggable) {
                HttpLoggingInterceptor.Level.BASIC
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
            redactHeader("Authorization")
            redactHeader("Cookie")
            redactHeader("Set-Cookie")
        }
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(loggingInterceptor: HttpLoggingInterceptor): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .callTimeout(30, TimeUnit.SECONDS)
            .addInterceptor(loggingInterceptor)
            .build()
    }

    @Provides
    @Singleton
    @Named("TelegramRetrofit")
    fun provideTelegramRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://api.telegram.org/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    @Named("WhatsAppRetrofit")
    fun provideWhatsAppRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://graph.facebook.com/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    @Named("GmailRetrofit")
    fun provideGmailRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://gmail.googleapis.com/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideTelegramApi(@Named("TelegramRetrofit") retrofit: Retrofit): TelegramApi {
        return retrofit.create(TelegramApi::class.java)
    }

    @Provides
    @Singleton
    fun provideWhatsAppApi(@Named("WhatsAppRetrofit") retrofit: Retrofit): WhatsAppApi {
        return retrofit.create(WhatsAppApi::class.java)
    }

    @Provides
    @Singleton
    fun provideGmailApi(@Named("GmailRetrofit") retrofit: Retrofit): GmailApi {
        return retrofit.create(GmailApi::class.java)
    }
}

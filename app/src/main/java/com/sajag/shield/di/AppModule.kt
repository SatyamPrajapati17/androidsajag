package com.sajag.shield.di

import android.content.Context
import android.media.AudioManager
import android.os.Vibrator
import android.telephony.TelephonyManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideAudioManager(
        @ApplicationContext context: Context
    ): AudioManager {
        return context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }

    @Provides
    @Singleton
    fun provideTelephonyManager(
        @ApplicationContext context: Context
    ): TelephonyManager {
        return context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
    }

    @Provides
    @Singleton
    fun provideVibrator(
        @ApplicationContext context: Context
    ): Vibrator {
        @Suppress("DEPRECATION")
        return context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }
}

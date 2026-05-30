package com.whatcable.android.di

import android.content.Context
import android.hardware.usb.UsbManager
import androidx.room.Room
import com.whatcable.android.data.db.CableTestDao
import com.whatcable.android.data.db.ChargingSampleDao
import com.whatcable.android.data.db.WhatCableDatabase
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
    fun provideUsbManager(@ApplicationContext context: Context): UsbManager {
        return context.getSystemService(Context.USB_SERVICE) as UsbManager
    }

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): WhatCableDatabase {
        return Room.databaseBuilder(
            context,
            WhatCableDatabase::class.java,
            "whatcable.db"
        )
            // Pre-release app with disposable charging history; drop and rebuild
            // on schema change rather than ship migrations.
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()
    }

    @Provides
    fun provideChargingSampleDao(db: WhatCableDatabase): ChargingSampleDao {
        return db.chargingSampleDao()
    }

    @Provides
    fun provideCableTestDao(db: WhatCableDatabase): CableTestDao {
        return db.cableTestDao()
    }
}

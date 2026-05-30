package com.whatcable.android.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [ChargingSample::class],
    version = 2,
    exportSchema = false
)
abstract class WhatCableDatabase : RoomDatabase() {
    abstract fun chargingSampleDao(): ChargingSampleDao
}

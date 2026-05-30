package com.whatcable.android.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [ChargingSample::class, CableTest::class],
    version = 3,
    exportSchema = false
)
abstract class WhatCableDatabase : RoomDatabase() {
    abstract fun chargingSampleDao(): ChargingSampleDao
    abstract fun cableTestDao(): CableTestDao
}

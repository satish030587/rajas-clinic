package com.rajashomoeocare.clinic.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [
        PatientEntity::class,
        VisitEntity::class,
        PhotoEntity::class,
        InvoiceEntity::class,
        MessageTemplateEntity::class,
        MessageLogEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class ClinicDatabase : RoomDatabase() {

    abstract fun patients(): PatientDao
    abstract fun visits(): VisitDao
    abstract fun photos(): PhotoDao
    abstract fun invoices(): InvoiceDao
    abstract fun templates(): TemplateDao
    abstract fun messageLog(): MessageLogDao

    companion object {
        fun build(context: Context): ClinicDatabase =
            Room.databaseBuilder(context, ClinicDatabase::class.java, "rajas_clinic.db")
                .build()
    }
}

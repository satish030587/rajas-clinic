package com.rajashomoeocare.clinic.data.local

import androidx.room.TypeConverter
import java.time.Instant
import java.time.LocalDate

class Converters {
    @TypeConverter fun toEpochDay(value: LocalDate?): Long? = value?.toEpochDay()

    @TypeConverter fun fromEpochDay(value: Long?): LocalDate? = value?.let(LocalDate::ofEpochDay)

    @TypeConverter fun toEpochMillis(value: Instant?): Long? = value?.toEpochMilli()

    @TypeConverter fun fromEpochMillis(value: Long?): Instant? = value?.let(Instant::ofEpochMilli)

    @TypeConverter fun toSex(value: Sex): String = value.name

    @TypeConverter fun fromSex(value: String): Sex = Sex.valueOf(value)

    @TypeConverter fun toLanguage(value: Language): String = value.name

    @TypeConverter fun fromLanguage(value: String): Language = Language.valueOf(value)

    @TypeConverter fun toPaymentMode(value: PaymentMode): String = value.name

    @TypeConverter fun fromPaymentMode(value: String): PaymentMode = PaymentMode.valueOf(value)

    @TypeConverter fun toTemplateKey(value: TemplateKey): String = value.name

    @TypeConverter fun fromTemplateKey(value: String): TemplateKey = TemplateKey.valueOf(value)

    @TypeConverter fun toChannel(value: MessageChannel): String = value.name

    @TypeConverter fun fromChannel(value: String): MessageChannel = MessageChannel.valueOf(value)
}

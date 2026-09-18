package com.rajashomoeocare.clinic.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.rajashomoeocare.clinic.data.remote.ApiService
import com.rajashomoeocare.clinic.data.remote.ClinicalUpdate
import com.rajashomoeocare.clinic.data.remote.MessageLogCreate
import com.rajashomoeocare.clinic.data.remote.StartVisitRequest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import retrofit2.HttpException
import java.io.IOException
import java.util.UUID

private val Context.outboxStore by preferencesDataStore("clinic_outbox")

/**
 * A write the clinic made while the network was down.
 *
 * Deliberately a short queue of whole requests rather than a sync engine
 * (spec v3 §3): the clinic is on reliable WiFi, and this exists so a router
 * reboot cannot block a consultation — not to support working offline all day.
 */
@Serializable
data class PendingWrite(
    val id: String = UUID.randomUUID().toString(),
    val kind: Kind,
    val payload: String,
    val targetId: String = "",
    val label: String = "",
    val createdAt: Long = System.currentTimeMillis(),
) {
    @Serializable
    enum class Kind { START_VISIT, SAVE_CLINICAL, LOG_MESSAGE }
}

class OutboxStore(private val context: Context) {

    private val key = stringPreferencesKey("pending")
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    val pending: Flow<List<PendingWrite>> =
        context.outboxStore.data.map { prefs -> decode(prefs[key]) }

    suspend fun add(entry: PendingWrite) {
        context.outboxStore.edit { prefs ->
            prefs[key] = json.encodeToString(decode(prefs[key]) + entry)
        }
    }

    suspend fun remove(id: String) {
        context.outboxStore.edit { prefs ->
            prefs[key] = json.encodeToString(decode(prefs[key]).filterNot { it.id == id })
        }
    }

    suspend fun snapshot(): List<PendingWrite> = pending.first()

    private fun decode(raw: String?): List<PendingWrite> =
        raw?.let { runCatching { json.decodeFromString<List<PendingWrite>>(it) }.getOrNull() }
            ?: emptyList()
}

/**
 * Drains the outbox oldest-first. Stops at the first network failure so order
 * is preserved — a clinical update must never overtake the visit it belongs to.
 */
class OutboxSync(
    private val api: ApiService,
    private val outbox: OutboxStore,
) {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    suspend fun drain(): Int {
        var sent = 0
        for (entry in outbox.snapshot().sortedBy { it.createdAt }) {
            try {
                when (entry.kind) {
                    PendingWrite.Kind.START_VISIT ->
                        api.startVisit(json.decodeFromString<StartVisitRequest>(entry.payload))

                    PendingWrite.Kind.SAVE_CLINICAL ->
                        api.updateClinical(
                            entry.targetId,
                            json.decodeFromString<ClinicalUpdate>(entry.payload),
                        )

                    PendingWrite.Kind.LOG_MESSAGE ->
                        api.logMessage(json.decodeFromString<MessageLogCreate>(entry.payload))
                }
                outbox.remove(entry.id)
                sent++
            } catch (_: IOException) {
                // Still offline. Leave this and everything after it queued.
                return sent
            } catch (e: HttpException) {
                // The server rejected it outright; retrying will not help, and
                // keeping it would block everything behind it forever.
                outbox.remove(entry.id)
            }
        }
        return sent
    }
}

package com.example.connect.logic

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.Serializer
import androidx.datastore.dataStore
import com.example.connect.proto.SessionState
import com.example.connect.proto.AppState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import java.io.InputStream
import java.io.OutputStream
import java.io.IOException

/**
 * Serializer for Protobuf SessionState.
 */
object SessionStateSerializer : Serializer<SessionState> {
    override val defaultValue: SessionState = SessionState.getDefaultInstance()

    override suspend fun readFrom(input: InputStream): SessionState {
        try {
            return SessionState.parseFrom(input)
        } catch (exception: Exception) {
            throw IOException("Cannot read proto.", exception)
        }
    }

    override suspend fun writeTo(t: SessionState, output: OutputStream) {
        t.writeTo(output)
    }
}

val Context.sessionDataStore: DataStore<SessionState> by dataStore(
    fileName = "session_state.pb",
    serializer = SessionStateSerializer
)

/**
 * Repository to manage Session Continuity.
 */
class SessionRepository(private val context: Context) {

    val sessionState: Flow<SessionState> = context.sessionDataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(SessionState.getDefaultInstance())
            } else {
                throw exception
            }
        }

    suspend fun updateSession(update: (SessionState.Builder) -> Unit) {
        context.sessionDataStore.updateData { current ->
            val builder = current.toBuilder()
            update(builder)
            builder.build()
        }
    }

    suspend fun clearSession() {
        context.sessionDataStore.updateData { SessionState.getDefaultInstance() }
    }
}

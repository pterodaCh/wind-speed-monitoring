package com.tetiana.wind_speed_monitoring.data.cache

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

//
//  Простий кеш у пам'яті з часом життя (TTL).
//  Mutex робить його безпечним при одночасних зверненнях з кількох корутин.
//  Використовується Repository, щоб не опитувати сенсор частіше ніж раз на TTL
//
class MemoryCache<K, V>(private val ttlMillis: Long) {

    private data class Entry<V>(val value: V, val savedAt: Long)

    private val entries = mutableMapOf<K, Entry<V>>()
    private val mutex = Mutex()

    // Повертає значення, лише якщо воно ще "свіже"
    suspend fun get(key: K): V? = mutex.withLock {
        val entry = entries[key] ?: return@withLock null
        if (System.currentTimeMillis() - entry.savedAt < ttlMillis) entry.value
        else {
            entries.remove(key)
            null
        }
    }

    suspend fun put(key: K, value: V) {
        mutex.withLock { entries[key] = Entry(value, System.currentTimeMillis()) }
    }

    suspend fun invalidate(key: K) {
        mutex.withLock { entries.remove(key) }
    }
}
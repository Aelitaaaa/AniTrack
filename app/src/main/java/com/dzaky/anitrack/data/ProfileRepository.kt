package com.dzaky.anitrack.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.dzaky.anitrack.domain.AnimeBadge
import com.dzaky.anitrack.domain.LocalProfile
import com.dzaky.anitrack.domain.ProfileInput
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.map

@Singleton
class ProfileRepository @Inject constructor(private val store: DataStore<Preferences>) {
    private val id = stringPreferencesKey("profile_id")
    private val name = stringPreferencesKey("profile_name")
    private val username = stringPreferencesKey("profile_username")
    private val bio = stringPreferencesKey("profile_bio")
    private val createdAt = longPreferencesKey("profile_created_at")
    private val badges = stringSetPreferencesKey("profile_badges")

    val profile = store.data.map { p -> p[id]?.let { LocalProfile(it, p[name].orEmpty(), p[username].orEmpty(), p[bio].orEmpty(), p[createdAt] ?: 0L) } }
    val earnedBadges = store.data.map { p -> AnimeBadge.entries.filter { it.name in p[badges].orEmpty() }.toSet() }

    suspend fun save(input: ProfileInput) {
        input.validationError()?.let { throw IllegalArgumentException(it) }
        val normalized = input.normalized()
        store.edit { p ->
            if (p[id] == null) {
                p[id] = UUID.randomUUID().toString()
                p[createdAt] = System.currentTimeMillis()
            }
            p[name] = normalized.displayName
            p[username] = normalized.username
            p[bio] = normalized.bio
            p[badges] = p[badges].orEmpty() + AnimeBadge.PendatangBaru.name
        }
    }

    suspend fun unlock(earned: Set<AnimeBadge>) {
        if (earned.isEmpty()) return
        store.edit { p ->
            if (p[id] != null) p[badges] = p[badges].orEmpty() + earned.map { it.name }
        }
    }
}

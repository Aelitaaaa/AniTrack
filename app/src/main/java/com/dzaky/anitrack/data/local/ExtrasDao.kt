package com.dzaky.anitrack.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface ExtrasDao {
    @Query("SELECT * FROM collections ORDER BY nameKey, id")
    fun observeCollections(): Flow<List<CollectionEntity>>
    @Query("SELECT * FROM collection_anime ORDER BY addedAt DESC, animeId")
    fun observeMembers(): Flow<List<CollectionAnimeEntity>>
    @Query("SELECT * FROM personal_anime ORDER BY updatedAt DESC, animeId")
    fun observePersonal(): Flow<List<PersonalAnimeEntity>>
    @Query("SELECT * FROM personal_anime WHERE animeId = :id")
    fun observePersonal(id: Int): Flow<PersonalAnimeEntity?>
    @Query("SELECT * FROM collections ORDER BY nameKey, id")
    suspend fun collections(): List<CollectionEntity>
    @Query("SELECT * FROM collection_anime")
    suspend fun members(): List<CollectionAnimeEntity>
    @Query("SELECT * FROM personal_anime")
    suspend fun personal(): List<PersonalAnimeEntity>
    @Query("SELECT * FROM collections WHERE id = :id")
    suspend fun collection(id: String): CollectionEntity?
    @Query("SELECT * FROM collections WHERE nameKey = :key")
    suspend fun byName(key: String): CollectionEntity?
    @Query("SELECT * FROM personal_anime WHERE animeId = :id")
    suspend fun personal(id: Int): PersonalAnimeEntity?
    @Upsert suspend fun saveCollection(collection: CollectionEntity)
    @Upsert suspend fun saveMember(member: CollectionAnimeEntity)
    @Upsert suspend fun savePersonal(personal: PersonalAnimeEntity)
    @Query("DELETE FROM collections WHERE id = :id") suspend fun deleteCollection(id: String)
    @Query("DELETE FROM collection_anime WHERE collectionId = :collectionId AND animeId = :animeId")
    suspend fun removeMember(collectionId: String, animeId: Int)
    @Query("DELETE FROM personal_anime WHERE animeId = :id") suspend fun removePersonal(id: Int)
}

package com.dzaky.anitrack.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS collections (id TEXT NOT NULL, name TEXT NOT NULL, nameKey TEXT NOT NULL, createdAt INTEGER NOT NULL, PRIMARY KEY(id))")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_collections_nameKey ON collections(nameKey)")
        db.execSQL("CREATE TABLE IF NOT EXISTS collection_anime (collectionId TEXT NOT NULL, animeId INTEGER NOT NULL, title TEXT NOT NULL, posterUrl TEXT, score REAL, type TEXT, year INTEGER, episodes INTEGER, addedAt INTEGER NOT NULL, PRIMARY KEY(collectionId, animeId), FOREIGN KEY(collectionId) REFERENCES collections(id) ON UPDATE NO ACTION ON DELETE CASCADE)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_collection_anime_collectionId ON collection_anime(collectionId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_collection_anime_animeId ON collection_anime(animeId)")
        db.execSQL("CREATE TABLE IF NOT EXISTS personal_anime (animeId INTEGER NOT NULL, title TEXT NOT NULL, rating INTEGER, notes TEXT NOT NULL, updatedAt INTEGER NOT NULL, PRIMARY KEY(animeId))")
    }
}

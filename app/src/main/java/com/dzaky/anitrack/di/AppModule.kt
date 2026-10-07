package com.dzaky.anitrack.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.room.Room
import com.dzaky.anitrack.data.local.AniTrackDatabase
import com.dzaky.anitrack.data.remote.JikanApi
import com.dzaky.anitrack.data.remote.AniListApi
import com.dzaky.anitrack.data.settingsDataStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.util.concurrent.TimeUnit
import javax.inject.Singleton
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton
    fun json(): Json = Json { ignoreUnknownKeys = true; coerceInputValues = true }

    @Provides
    @Singleton
    fun client(): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(6, TimeUnit.SECONDS)
        .readTimeout(12, TimeUnit.SECONDS)
        .callTimeout(18, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    @Provides
    @Singleton
    fun api(client: OkHttpClient, json: Json): JikanApi = Retrofit.Builder()
        .baseUrl("https://api.jikan.moe/v4/")
        .client(client)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build().create(JikanApi::class.java)

    @Provides
    @Singleton
    fun alternateApi(client: OkHttpClient, json: Json): AniListApi = Retrofit.Builder()
        .baseUrl("https://graphql.anilist.co/")
        .client(client)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build().create(AniListApi::class.java)

    @Provides
    @Singleton
    fun database(@ApplicationContext context: Context): AniTrackDatabase = Room.databaseBuilder(
        context, AniTrackDatabase::class.java, "anitrack.db",
    ).build()

    @Provides fun favorites(database: AniTrackDatabase) = database.favorites()
    @Provides fun watchlist(database: AniTrackDatabase) = database.watchlist()
    @Provides fun history(database: AniTrackDatabase) = database.history()
    @Provides fun cache(database: AniTrackDatabase) = database.animeCache()

    @Provides
    @Singleton
    fun preferences(@ApplicationContext context: Context): DataStore<Preferences> = context.settingsDataStore
}

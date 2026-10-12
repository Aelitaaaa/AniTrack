package com.dzaky.anitrack.data

import com.dzaky.anitrack.data.backup.BackupCodec
import com.dzaky.anitrack.data.backup.BackupDocument
import com.dzaky.anitrack.data.local.CollectionAnimeEntity
import com.dzaky.anitrack.data.local.CollectionEntity
import com.dzaky.anitrack.data.local.PersonalAnimeEntity
import com.dzaky.anitrack.domain.AnimeBadge
import com.dzaky.anitrack.domain.LocalProfile
import java.io.ByteArrayInputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class BackupCodecTest {
    @Test fun backupPreservesUnicodeNotesProfileAndPreferences() {
        val document = BackupDocument(createdAt = 1,
            collections = listOf(CollectionEntity("shelf", "Weekend", "weekend", 1)),
            members = listOf(CollectionAnimeEntity("shelf", 20, "Naruto", null, 7.9, "TV", 2002, 220, 1)),
            personal = listOf(PersonalAnimeEntity(20, "Naruto", 8, "猫\nRewatch", 1)),
            profile = ProfileSnapshot(LocalProfile("profile", "Dzaky", "ykazd", "Anime", 1), setOf(AnimeBadge.Maraton)),
            preferences = AppPreferences(ThemeMode.Black, true, LibraryLayout.Grid, CatalogSource.AniList))
        assertEquals(document, BackupCodec.decode(BackupCodec.encode(document)))
    }

    @Test fun unsupportedAndUnmarkedJsonIsRejected() {
        assertThrows(IllegalArgumentException::class.java) { BackupCodec.decode("{}".toByteArray()) }
        assertThrows(IllegalArgumentException::class.java) { BackupCodec.decode("""{"format":"AniTrack","version":99}""".toByteArray()) }
        assertThrows(IllegalArgumentException::class.java) { BackupCodec.decode("""{"format":"Other","version":1}""".toByteArray()) }
    }

    @Test fun danglingAndDuplicateMembershipsAreRejected() {
        val member = CollectionAnimeEntity("missing", 1, "Title", null, null, null, null, null, 1)
        assertThrows(IllegalArgumentException::class.java) { BackupCodec.encode(BackupDocument(members = listOf(member))) }
        val collection = CollectionEntity("missing", "Shelf", "shelf", 1)
        assertThrows(IllegalArgumentException::class.java) { BackupCodec.encode(BackupDocument(collections = listOf(collection), members = listOf(member, member))) }
    }

    @Test fun inputLimitIsCheckedBeforeParsing() {
        val bytes = ByteArray(BackupCodec.MaxBytes + 1)
        assertThrows(IllegalArgumentException::class.java) { BackupCodec.readLimited(ByteArrayInputStream(bytes)) }
    }
}
